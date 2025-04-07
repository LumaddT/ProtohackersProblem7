package line.reversal.TransportLayer;

import lombok.RequiredArgsConstructor;

import java.net.InetAddress;

@RequiredArgsConstructor
public class LRCPSocket {
    private final InetAddress RemoteIP;
    private final int RemotePort;

    public String getLine() {
        return null;
    }

    public void sendLine(String line) {
        // TODO
    }

    public void close() {
        // TODO
    }
}
