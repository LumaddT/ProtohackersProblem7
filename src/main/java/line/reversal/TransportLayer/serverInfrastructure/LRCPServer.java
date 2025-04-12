package line.reversal.TransportLayer.serverInfrastructure;

import line.reversal.TransportLayer.messages.ClientMessage;
import line.reversal.TransportLayer.messages.ServerMessage;
import line.reversal.TransportLayer.exceptions.IllegalMessageFormattingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LRCPServer implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger();

    public static final int MAX_LENGTH = 1_000;

    private int Timeout = 0;
    private boolean Alive;

    private final UDPSocketHolder UDPSocketHolder;
    public final Map<Integer, LRCPSocket> Sockets = new ConcurrentHashMap<>();
    public final BlockingQueue<LRCPSocket> SocketQueue = new LinkedBlockingQueue<>();

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
            } catch (IOException e) {
                logger.fatal("An IO exception was thrown from the UDP socket while receiving. Message: {}.", e.getMessage());
                this.close();
                return;
            }

            ClientMessage clientMessage;
            try {
                clientMessage = MessageParser.parseClientMessage(buffer);
            } catch (IllegalMessageFormattingException e) {
                logger.debug(e.getMessage());
                continue;
            }

            logger.debug("Received {}.", clientMessage.toString());
            int sessionId = clientMessage.getSessionId();

            if (!Sockets.containsKey(sessionId)) {
                LRCPSocket newSocket = new LRCPSocket(sessionId, clientPacket.getAddress(), clientPacket.getPort(), this);
                Sockets.put(sessionId, newSocket);
                SocketQueue.add(newSocket);
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

    @Override
    public void close() {
        Alive = false;

        Sockets.values().forEach(LRCPSocket::close);
        Sockets.clear();

        UDPSocketHolder.close();
    }

    public void send(ServerMessage message, InetAddress remoteIP, int remotePort) {
        byte[] encodedMessage = message.encode();

        DatagramPacket packet = new DatagramPacket(encodedMessage, encodedMessage.length, remoteIP, remotePort);

        try {
            UDPSocketHolder.send(packet);
        } catch (IOException e) {
            logger.fatal("An IO exception was thrown from the UDP socket while sending. Message: {}.", e.getMessage());
            this.close();
        }
    }
}
