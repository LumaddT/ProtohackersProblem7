package line.reversal.TransportLayer.messages;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MessageTypes {
    CONNECT("connect"),
    DATA("data"),
    ACK("ack"),
    CLOSE("close");

    private final String Identifier;
}
