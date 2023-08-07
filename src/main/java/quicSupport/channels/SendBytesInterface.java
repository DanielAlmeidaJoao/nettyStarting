package quicSupport.channels;

public interface SendBytesInterface<T> {

    void send(String streamId, T message);

}
