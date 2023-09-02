package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.net.InetSocketAddress;

public interface UDPChannelConsumer {

    void deliverMessage(ByteBuf message, InetSocketAddress from);
    void messageSentHandler(boolean success, Throwable error, BabelMessage message, InetSocketAddress dest);

    void peerDown(InetSocketAddress peer);
}
