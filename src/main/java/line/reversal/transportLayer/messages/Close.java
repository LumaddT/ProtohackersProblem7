package line.reversal.transportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Close implements Message {
    private final MessageTypes MessageType = MessageTypes.CLOSE;

    private final int SessionId;

    @Override
    public String toString() {
        return "/%s/%d/".formatted(MessageType.getIdentifier(), SessionId);
    }
}
