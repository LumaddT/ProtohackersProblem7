package line.reversal.TransportLayer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPSocketHolder {
    private final DatagramSocket Socket;

    public UDPSocketHolder(int port) throws SocketException {
        Socket = new DatagramSocket(port);
    }

    public synchronized void send(DatagramPacket datagramPacket) throws IOException {
        Socket.send(datagramPacket);
    }

    public void receive(DatagramPacket clientPacket) throws IOException {
        Socket.receive(clientPacket);
    }

    /**
     * Timeout in ms
     */
    public void setSoTimeout(int timeout) throws SocketException {
        Socket.setSoTimeout(timeout);
    }

    public void close() {
        Socket.close();
    }
}
