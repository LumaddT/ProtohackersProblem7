package line.reversal.transportLayer.serverInfrastructure;

import line.reversal.transportLayer.messages.Close;
import line.reversal.transportLayer.messages.Message;
import line.reversal.transportLayer.exceptions.IllegalMessageFormattingException;
import line.reversal.transportLayer.messages.MessageTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LRCPServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger();

    static final int MAX_LENGTH = 1_000;

    private volatile int Timeout = 0;
    private volatile boolean Alive;

    private final UDPSocketHolder UDPSocketHolder;
    private final Map<Integer, LRCPSocket> Sockets = new ConcurrentHashMap<>();
    private final BlockingQueue<LRCPSocket> SocketQueue = new LinkedBlockingQueue<>();

    public LRCPServer(int port) throws SocketException {
        UDPSocketHolder = new UDPSocketHolder(port);
        Alive = true;

        new Thread(this::run).start();
    }

    private void run() {
        try {
            UDPSocketHolder.setSoTimeout(1_000);
        } catch (SocketException e) {
            logger.fatal("An error occurred while configuring the UDP socket. Message: {}", e.getMessage());
            this.close();
            return;
        }

        while (Alive) {
            byte[] buffer = new byte[MAX_LENGTH];
            DatagramPacket clientPacket = new DatagramPacket(buffer, MAX_LENGTH);
            try {
                UDPSocketHolder.receive(clientPacket);
            } catch (SocketTimeoutException e) {
                continue;
            }

            Message clientMessage;
            try {
                clientMessage = MessageParser.parseClientMessage(buffer);
            } catch (IllegalMessageFormattingException e) {
                logger.debug("String \"{}\" was not accepted. Error message: {}", new String(buffer, StandardCharsets.US_ASCII), e.getMessage());
                continue;
            }

            logger.info("Received valid message {}.", clientMessage.toString());
            int sessionId = clientMessage.getSessionId();

            if (!Sockets.containsKey(sessionId) && clientMessage.getMessageType() == MessageTypes.CONNECT) {
                LRCPSocket newSocket = new LRCPSocket(sessionId, clientPacket.getAddress(), clientPacket.getPort(), this);
                Sockets.put(sessionId, newSocket);
                SocketQueue.add(newSocket);
            } else if (!Sockets.containsKey(sessionId)) {
                Close close = new Close(sessionId);
                this.send(close, clientPacket.getAddress(), clientPacket.getPort());
            } else {
                Sockets.get(sessionId).incomingMessage(clientMessage);
            }
        }
    }

    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds. With this option set to a positive timeout value, a call to accept() for this ServerSocket will block for only this amount of time. If the timeout expires, a <b>java. net. SocketTimeoutException</b> is raised, though the ServerSocket is still valid. A timeout of zero is interpreted as an infinite timeout. The option <b>must</b> be enabled prior to entering the blocking operation to have effect.
     *
     * @param timeout the specified timeout, in milliseconds
     * @throws IllegalArgumentException if <code>timeout</code> is negative
     */
    public void setSoTimeout(int timeout) throws IllegalArgumentException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout %d provided to LRCPServer.setSoTimeout().".formatted(timeout));
        }

        Timeout = timeout;
    }

    public LRCPSocket accept() throws IOException {
        try {
            LRCPSocket socket = SocketQueue.poll(Timeout, TimeUnit.MILLISECONDS);

            if (socket == null) {
                throw new SocketTimeoutException();
            }

            return socket;
        } catch (InterruptedException e) {
            logger.fatal("The thread of the SocketQueue in LRCPServer.accept() was interrupted prematurely.");
            throw new IOException("The thread of the SocketQueue in LRCPServer.accept() was interrupted prematurely.");
        }
    }

    void send(Message message, InetAddress remoteIP, int remotePort) {
        send(message, remoteIP, remotePort, false);
    }

    void send(Message message, InetAddress remoteIP, int remotePort, boolean isRetransmitted) {
        byte[] encodedMessage = message.toString().getBytes();

        if (encodedMessage.length > MAX_LENGTH) {
            logger.fatal("Attempted to send a message longer than the maximum limit. Message sent: {}", message.toString());
            this.close();
            return;
        }

        DatagramPacket packet = new DatagramPacket(encodedMessage, encodedMessage.length, remoteIP, remotePort);

        if (!isRetransmitted) {
            logger.info("Sending {}", message.toString());
        } else {
            logger.info("Retransmitting {}", message.toString());
        }
        UDPSocketHolder.send(packet);
    }

    @Override
    public void close() {
        Alive = false;

        Sockets.values().forEach(LRCPSocket::close);
        Sockets.clear();

        UDPSocketHolder.close();
    }

    void removeSession(int sessionId) {
        if (Sockets.containsKey(sessionId)) {
            //noinspection ResultOfMethodCallIgnored
            SocketQueue.remove(Sockets.get(sessionId));
            Sockets.remove(sessionId);
        }
    }
}
