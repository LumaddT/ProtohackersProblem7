package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Close implements ClientMessage, ServerMessage {
    private final MessageTypes MessageType = MessageTypes.CLOSE;

    private final int SessionId;
}
