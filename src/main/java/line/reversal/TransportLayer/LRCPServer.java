package line.reversal.TransportLayer;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

public class LRCPServer implements AutoCloseable {
    public static final int MAX_LENGTH = 1_000;

    public final Set<LRCPSocket> Sockets = new HashSet<>();

    private final UDPSocketHolder UDPSocketHolder;

    public LRCPServer(int port) throws SocketException {
        UDPSocketHolder = new UDPSocketHolder(port);
    }

    public void setSoTimeout(int timeout) {
        // TODO
    }

    public LRCPSocket accept() throws SocketTimeoutException {
        //TODO

        return null;
    }

    @Override
    public void close() {
        Sockets.forEach(LRCPSocket::close);

        UDPSocketHolder.close();
    }
}
