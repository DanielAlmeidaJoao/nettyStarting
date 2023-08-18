package udpSupport.channels;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

public interface UDPChannelConsumer<T> {

    void deliverMessage(ByteBuf message, InetSocketAddress from);
    void messageSentHandler(boolean success, Throwable error, T message, InetSocketAddress dest);

    void peerDown(InetSocketAddress peer);
}
