package udpSupport.channels;

import java.net.InetSocketAddress;

public interface UDPChannelHandlerMethods<T> {
    void onPeerDown(InetSocketAddress peer);
    void onDeliverMessage(T message, InetSocketAddress from);
    void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest);
}
