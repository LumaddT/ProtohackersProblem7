package line.reversal.TransportLayer.serverInfrastructure;

import line.reversal.TransportLayer.messages.*;
import line.reversal.TransportLayer.exceptions.IllegalMessageFormattingException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

class MessageParser {
    private static final Set<Character> ESCAPABLE_CHARS = Set.of('\\', '/', 'n');

    static Message parseClientMessage(byte[] clientMessage) throws IllegalMessageFormattingException {
        char[] messageChars = new String(clientMessage, StandardCharsets.US_ASCII).toCharArray();

        // With this check I can simply check the char at i+1 when I have a '\' in the message
        // without worrying about going out of bounds
        if (messageChars[messageChars.length - 1] == '\\') {
            throw new IllegalMessageFormattingException("The last character of the message is '\\'.");
        }

        StringBuilder messageTypeString = new StringBuilder();
        StringBuilder sessionIdString = new StringBuilder();
        StringBuilder field1String = new StringBuilder();
        StringBuilder field2String = new StringBuilder();

        State state = State.MESSAGE_START;

        for (int i = 0; i < messageChars.length; i++) {
            char ch = messageChars[i];

            if (ch == '/') {
                state = switch (state) {
                    case MESSAGE_START -> State.MESSAGE_TYPE;
                    case MESSAGE_TYPE -> State.SESSION_ID;
                    case SESSION_ID -> State.FIELD_1;
                    case FIELD_1 -> State.FIELD_2;
                    case FIELD_2 -> State.END;
                    case END ->
                            throw new IllegalMessageFormattingException("Found extra characters after end of message was reached.");
                };

                continue;
            }

            if (ch == '\\') {
                if (!ESCAPABLE_CHARS.contains(messageChars[i + 1])) {
                    throw new IllegalMessageFormattingException("Attempted to escape illegal char %c.".formatted(messageChars[i + 1]));
                }

                if (messageChars[i + 1] == 'n') {
                    ch = '\n';
                } else {
                    ch = messageChars[i + 1];
                }

                i++;
            }

            switch (state) {
                case MESSAGE_START ->
                        throw new IllegalMessageFormattingException("The first character of the message was is not '/'.");
                case MESSAGE_TYPE -> messageTypeString.append(ch);
                case SESSION_ID -> sessionIdString.append(ch);
                case FIELD_1 -> field1String.append(ch);
                case FIELD_2 -> field2String.append(ch);
                case END -> throw new IllegalMessageFormattingException("Message went on after end was reached.");
            }
        }

        MessageTypes messageType = null;
        for (MessageTypes candidate : MessageTypes.values()) {
            if (candidate.getIdentifier().contentEquals(messageTypeString)) {
                messageType = candidate;
                break;
            }
        }

        if (messageType == null) {
            throw new IllegalMessageFormattingException("Illegal message type %s received.".formatted(messageTypeString.toString()));
        }

        switch (messageType) {
            case CONNECT -> {
                if (state != State.FIELD_1 || !field1String.isEmpty()) {
                    throw new IllegalMessageFormattingException("CONNECT message the wrong amount of fields.");
                }
            }
            case DATA -> {
                if (state != State.END) {
                    throw new IllegalMessageFormattingException("DATA message the wrong amount of fields.");
                }
            }
            case ACK -> {
                if (state != State.FIELD_2 || !field2String.isEmpty()) {
                    throw new IllegalMessageFormattingException("ACK message the wrong amount of fields.");
                }
            }
            case CLOSE -> {
                if (state != State.FIELD_1 || !field1String.isEmpty()) {
                    throw new IllegalMessageFormattingException("CLOSE message the wrong amount of fields.");
                }
            }
        }

        int sessionId = parseNumericField(sessionIdString.toString());

        switch (messageType) {
            case CONNECT -> {
                return new Connect(sessionId);
            }
            case DATA -> {
                int position = parseNumericField(field1String.toString());
                return new Data(sessionId, position, field2String.toString());
            }
            case ACK -> {
                int length = parseNumericField(field1String.toString());
                return new Ack(sessionId, length);
            }
            case CLOSE -> {
                return new Close(sessionId);
            }
            default ->
                    throw new IllegalStateException("Unexpected value: %s. Impossible branch.".formatted(messageType));
        }
    }

    private static int parseNumericField(String string) throws IllegalMessageFormattingException {
        int parsed;
        try {
            parsed = Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new IllegalMessageFormattingException("Attempted to parse illegal number %s.".formatted(string));
        }

        if (parsed < 0) {
            throw new IllegalMessageFormattingException("Numeric fields must be non-negative integers. %d is illegal.".formatted(parsed));
        }

        return parsed;
    }

    private enum State {
        MESSAGE_START,
        MESSAGE_TYPE,
        SESSION_ID,
        FIELD_1,
        FIELD_2,
        END
    }
}
