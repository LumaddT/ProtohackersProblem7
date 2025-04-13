package line.reversal.transportLayer.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Data implements Message {
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

    public List<Data> split(int maxLength) {
        if (maxLength < 101) {
            throw new RuntimeException("Illegal maxLength set in Data.split()");
        }

        int effectiveMaxLength = maxLength - 100;

        List<Data> returnValue = new ArrayList<>();

        String payload = Payload;
        int position = Position;

        while (payload.length() > effectiveMaxLength) {
            returnValue.add(new Data(SessionId, position, payload.substring(0, effectiveMaxLength)));
            position += effectiveMaxLength;
            payload = payload.substring(effectiveMaxLength);
        }

        return Collections.unmodifiableList(returnValue);
    }
}
