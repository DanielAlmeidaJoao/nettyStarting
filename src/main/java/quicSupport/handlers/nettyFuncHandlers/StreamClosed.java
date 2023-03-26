package quicSupport.handlers.nettyFuncHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

@FunctionalInterface
public interface StreamClosed {
    void execute(QuicStreamChannel channel);

}
