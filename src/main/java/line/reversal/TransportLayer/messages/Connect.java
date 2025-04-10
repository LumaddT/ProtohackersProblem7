package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Connect implements ClientMessage {
    private final MessageTypes MessageType = MessageTypes.CONNECT;

    private final int SessionId;
}
