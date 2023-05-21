package udpSupport.channels;

import java.net.InetSocketAddress;

public interface UDPChannelHandlerMethods {
    void onPeerDown(InetSocketAddress peer);
    void onDeliverMessage(byte[] message, InetSocketAddress from);
    void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest);
}
