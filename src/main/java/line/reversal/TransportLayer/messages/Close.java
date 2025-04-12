package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
public class Close extends ServerMessage implements ClientMessage {
    private final MessageTypes MessageType = MessageTypes.CLOSE;

    private final int SessionId;

    @Override
    public String toString() {
        return "/%s/%d/".formatted(MessageType.getIdentifier(), SessionId);
    }
}
