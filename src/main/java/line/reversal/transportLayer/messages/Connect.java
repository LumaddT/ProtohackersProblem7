package line.reversal.transportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Connect implements Message {
    private final MessageTypes MessageType = MessageTypes.CONNECT;

    private final int SessionId;

    @Override
    public String toString() {
        return "/%s/%d/".formatted(MessageType.getIdentifier(), SessionId);
    }
}
