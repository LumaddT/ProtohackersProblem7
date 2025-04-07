package line.reversal.TransportLayer;

import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class LRCPSocket {
    private final int SessionId;
    private final InetAddress RemoteIP;
    private final int RemotePort;

    public final BlockingQueue<String> LinesQueue = new LinkedBlockingQueue<>();

    public String getLine() {
        // TODO: Timeout
        return LinesQueue.poll();
    }

    public void sendLine(String line) {
        // TODO
    }

    public void close() {
        // TODO
    }
}
