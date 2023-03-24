package quicSupport.handlers.funcHandlers;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicChannel;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ClientConnectHandler {
    void execute(boolean success, Throwable cause,InetSocketAddress remote);
}
