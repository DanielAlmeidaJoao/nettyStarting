package quicSupport.channels;

public interface SendBytesInterface {

    void send(String streamId, byte[] message,int len);

}
