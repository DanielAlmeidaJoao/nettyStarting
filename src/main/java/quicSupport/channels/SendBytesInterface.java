package quicSupport.channels;

import quicSupport.utils.enums.TransmissionType;

public interface SendBytesInterface {

    void send(String streamId, byte[] message,int len, TransmissionType type);

}
