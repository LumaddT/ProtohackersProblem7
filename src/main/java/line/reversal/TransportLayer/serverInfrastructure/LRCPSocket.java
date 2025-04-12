package line.reversal.TransportLayer.serverInfrastructure;

import line.reversal.TransportLayer.messages.Ack;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LRCPSocket {
    private static final int RETRANSMISSION_TIMEOUT_MS = 3_000;
    private static final int SESSION_EXPIRY_TIMEOUT_MS = 60_000;

    private final int SessionId;
    private final InetAddress RemoteIP;
    private final int RemotePort;

    private final LRCPServer ParentServer;

    private long LastPacketTimestamp;

    public final BlockingQueue<String> LinesQueue = new LinkedBlockingQueue<>();

    LRCPSocket(int sessionId, InetAddress remoteIP, int remotePort, LRCPServer parentServer) {
        SessionId = sessionId;
        RemoteIP = remoteIP;
        RemotePort = remotePort;
        ParentServer = parentServer;
        LastPacketTimestamp = System.currentTimeMillis() / 1000L;

        Ack ack = new Ack(SessionId, 0);

        ParentServer.send(ack, RemoteIP, RemotePort);
    }

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
