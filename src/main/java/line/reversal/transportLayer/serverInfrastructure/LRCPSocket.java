package line.reversal.transportLayer.serverInfrastructure;

import line.reversal.transportLayer.messages.Ack;
import line.reversal.transportLayer.messages.Close;
import line.reversal.transportLayer.messages.Data;
import line.reversal.transportLayer.messages.Message;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.*;

public class LRCPSocket {
    private static final Logger logger = LogManager.getLogger();

    private static final int RETRANSMISSION_TIMEOUT_MS = 3_000;
    private static final int SESSION_EXPIRY_TIMEOUT_MS = 60_000;

    @Getter
    private boolean Alive;

    private final int SessionId;
    private final InetAddress RemoteIP;
    private final int RemotePort;

    private final LRCPServer ParentServer;

    private long LastClientActionTimestampMillis;

    private int LastByteServerAcknowledged = 0;
    private int LastByteClientAcknowledged = 0;
    private int LastByteSent = 0;

    private final BlockingQueue<String> ClientLinesQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Data> ServerDataMessagesQueue = new LinkedBlockingQueue<>();
    private String IncompleteLine = null;

    LRCPSocket(int sessionId, InetAddress remoteIP, int remotePort, LRCPServer parentServer) {
        SessionId = sessionId;
        RemoteIP = remoteIP;
        RemotePort = remotePort;
        ParentServer = parentServer;
        LastClientActionTimestampMillis = System.currentTimeMillis();

        Alive = true;

        this.sendAck(0);

        new Thread(this::connectionTimeoutChecker).start();
        new Thread(this::retransmissionCheck).start();
    }

    private void connectionTimeoutChecker() {
        while (Alive) {
            long now = System.currentTimeMillis();
            long timeDiff = now - LastClientActionTimestampMillis;

            if (timeDiff > SESSION_EXPIRY_TIMEOUT_MS) {
                this.closeConnection();
            } else {
                try {
                    //noinspection BusyWait
                    Thread.sleep(SESSION_EXPIRY_TIMEOUT_MS - timeDiff + 10);
                } catch (InterruptedException e) {
                    // If this happens I have bigger issues than closing things gracefully
                    throw new RuntimeException(e);
                }
            }
        }
    }

    synchronized void incomingMessage(Message clientMessage) {
        LastClientActionTimestampMillis = System.currentTimeMillis();

        switch (clientMessage.getMessageType()) {
            case CONNECT -> this.sendAck(0);
            case DATA -> this.processData((Data) clientMessage);
            case ACK -> this.processAck((Ack) clientMessage);
            case CLOSE -> this.closeConnection();
        }
    }

    private void processData(Data clientMessage) {
        if (clientMessage.getPosition() != LastByteServerAcknowledged) {
            this.sendAck(LastByteServerAcknowledged);
            return;
        }

        int length = clientMessage.getPayload().length();
        this.sendAck(LastByteServerAcknowledged + length);
        LastByteServerAcknowledged += length;

        String[] lines = clientMessage.getPayload().split("(?<=\n)");
        for (String line : lines) {
            if (IncompleteLine != null) {
                line = IncompleteLine + line;
                IncompleteLine = null;
            }

            if (line.isEmpty()) {
                continue;
            }

            if (line.charAt(line.length() - 1) != '\n') {
                IncompleteLine = line;
            } else {
                ClientLinesQueue.add(line.substring(0, line.length() - 1));
            }
        }
    }

    private void processAck(Ack ack) {
        int position = ack.getPosition();

        if (position > LastByteSent) {
            this.close();
            return;
        }

        LastByteClientAcknowledged = position;
    }

    /**
     * Returns null when it times out
     */
    public String getLine(int timeoutMs) {
        try {
            return ClientLinesQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logger.fatal("The ClientLinesQueue threw InterruptException while polling. Error message: {}", e.getMessage());
            ParentServer.close();
            return null;
        }
    }

    public void sendLine(String line) {
        Data data = new Data(SessionId, LastByteSent, line + "\n");

        List<Data> splitDatas = data.split(LRCPServer.MAX_LENGTH);

        for (Data splitData : splitDatas) {
            ServerDataMessagesQueue.add(splitData);
            LastByteSent += splitData.getPayload().length();
        }
    }

    private void sendAck(int position) {
        Ack ack = new Ack(SessionId, position);

        ParentServer.send(ack, RemoteIP, RemotePort);
    }

    private void retransmissionCheck() {
        while (Alive) {
            Data data;
            try {
                data = ServerDataMessagesQueue.poll(1_000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (data == null) {
                continue;
            }

            boolean isRetransmitted = false;
            while (Alive) {
                if (LastByteClientAcknowledged == data.getPosition()) {
                    ParentServer.send(data, RemoteIP, RemotePort, isRetransmitted);
                } else if (LastByteClientAcknowledged >= data.getPosition() + data.getPayload().length()) {
                    break;
                }

                isRetransmitted = true;

                try {
                    //noinspection BusyWait
                    Thread.sleep(RETRANSMISSION_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void sendClose() {
        Close close = new Close(SessionId);

        ParentServer.send(close, RemoteIP, RemotePort);
    }

    /**
     * Remove socket from server.
     */
    void close() {
        Alive = false;
        ParentServer.removeSession(SessionId);
    }

    /**
     * Send the CLOSE message and remove socket from server.
     */
    public void closeConnection() {
        this.sendClose();
        this.close();
    }
}
