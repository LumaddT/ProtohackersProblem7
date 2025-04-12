package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Ack implements Message {
    private final MessageTypes MessageType = MessageTypes.ACK;

    private final int SessionId;
    private final int Position;

    @Override
    public String toString() {
        return "/%s/%d/%d/".formatted(MessageType.getIdentifier(), SessionId, Position);
    }
}
