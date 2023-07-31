package quicSupport.channels;

import io.netty.buffer.ByteBuf;

public interface SendBytesInterface {

    void send(String streamId, ByteBuf message);

}
