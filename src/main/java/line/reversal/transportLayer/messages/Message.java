package line.reversal.transportLayer.messages;

public interface Message {
    int getSessionId();

    MessageTypes getMessageType();
}
