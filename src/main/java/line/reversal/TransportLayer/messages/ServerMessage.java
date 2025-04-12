package line.reversal.TransportLayer.messages;

import java.nio.charset.StandardCharsets;

public abstract class ServerMessage {
    byte[] encode() {
        return this.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
