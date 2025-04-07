package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Ack implements ClientMessage, ServerMessage {
    private final int SessionId;
    private final int Length;
}
