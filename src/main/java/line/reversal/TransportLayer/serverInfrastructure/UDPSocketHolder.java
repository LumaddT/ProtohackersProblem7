package line.reversal.TransportLayer.serverInfrastructure;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

class UDPSocketHolder {
    private final DatagramSocket Socket;

    UDPSocketHolder(int port) throws SocketException {
        Socket = new DatagramSocket(port);
    }

    synchronized void send(DatagramPacket datagramPacket) throws IOException {
        Socket.send(datagramPacket);
    }

    synchronized void receive(DatagramPacket clientPacket) throws IOException {
        Socket.receive(clientPacket);
    }

    /**
     * Timeout in ms
     */
    @SuppressWarnings("SameParameterValue")
    void setSoTimeout(int timeout) throws SocketException {
        Socket.setSoTimeout(timeout);
    }

    void close() {
        Socket.close();
    }
}
