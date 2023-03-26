package quicSupport.handlers.nettyFuncHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface ConnectionActive {
    void execute(QuicStreamChannel channel, byte [] controlData,InetSocketAddress remotePeer);
}
