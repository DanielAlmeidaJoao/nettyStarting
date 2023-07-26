package udpSupport.channels;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

public interface UDPChannelHandlerMethods {
    void onPeerDown(InetSocketAddress peer);
    void onDeliverMessage(ByteBuf message, InetSocketAddress from);
    void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest);
}
