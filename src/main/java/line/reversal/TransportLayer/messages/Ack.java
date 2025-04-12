package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString
public class Ack implements ClientMessage, ServerMessage {
    private final MessageTypes MessageType = MessageTypes.ACK;

    private final int SessionId;
    private final int Length;
}
