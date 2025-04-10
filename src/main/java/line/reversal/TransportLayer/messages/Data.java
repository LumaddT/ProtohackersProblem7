package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Data implements ClientMessage, ServerMessage {
    private final MessageTypes MessageType = MessageTypes.DATA;

    private final int SessionId;
    private final int Position;
    private final String Payload;
}
