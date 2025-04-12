package line.reversal.TransportLayer.messages;

public interface Message {
    int getSessionId();

    MessageTypes getMessageType();
}
