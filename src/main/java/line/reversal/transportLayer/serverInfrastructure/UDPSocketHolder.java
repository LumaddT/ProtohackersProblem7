package line.reversal.transportLayer.serverInfrastructure;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

class UDPSocketHolder {
    private static final Logger logger = LogManager.getLogger();

    private final DatagramSocket Socket;

    @Getter
    private volatile boolean Alive;

    UDPSocketHolder(int port) throws SocketException {
        Socket = new DatagramSocket(port);
        Alive = true;
    }

    void send(DatagramPacket datagramPacket) {
        try {
            Socket.send(datagramPacket);
        } catch (IOException e) {
            logger.fatal("An IO exception was thrown from the UDP socket while sending. Error message: {}.", e.getMessage());
            this.close();
        }
    }

    synchronized void receive(DatagramPacket clientPacket) throws SocketTimeoutException {
        try {
            Socket.receive(clientPacket);
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            logger.fatal("An IO exception was thrown from the UDP socket while receiving. Message: {}.", e.getMessage());
            this.close();
        }
    }

    /**
     * Timeout in ms
     */
    @SuppressWarnings("SameParameterValue")
    void setSoTimeout(int timeout) throws SocketException {
        Socket.setSoTimeout(timeout);
    }

    void close() {
        Alive = false;
        Socket.close();
    }
}
