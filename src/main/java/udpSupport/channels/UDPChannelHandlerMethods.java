package udpSupport.channels;

import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.net.InetSocketAddress;

public interface UDPChannelHandlerMethods {
    void onPeerDown(InetSocketAddress peer);
    void onDeliverMessage(BabelMessage message, InetSocketAddress from);
    void onMessageSentHandler(boolean success, Throwable error, BabelMessage message, InetSocketAddress dest);
}
