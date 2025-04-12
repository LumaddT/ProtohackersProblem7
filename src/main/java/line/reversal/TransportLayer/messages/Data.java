package line.reversal.TransportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
public class Data extends ServerMessage implements ClientMessage {
    private final MessageTypes MessageType = MessageTypes.DATA;

    private final int SessionId;
    private final int Position;
    private final String Payload;

    @Override
    public String toString() {
        String escapedPayload = escape(Payload);
        return "/%s/%d/%d/%s/".formatted(MessageType.getIdentifier(), SessionId, Position, escapedPayload);
    }

    private static String escape(String payload) {
        return payload.replaceAll("\\\\", "\\\\\\\\") // All backslashes become double backslashes
                .replaceAll("/", "\\\\/") // All slashes become backslash-slash
                .replaceAll("\n", "\\\\n"); // All line feeds become backslash-n
    }
}
