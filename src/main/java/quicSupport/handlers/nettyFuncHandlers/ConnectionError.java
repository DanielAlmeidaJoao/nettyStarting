package quicSupport.handlers.nettyFuncHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ConnectionError {
    void execute(InetSocketAddress channelId,Throwable cause);
}
