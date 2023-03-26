package quicSupport.handlers.nettyFuncHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

@FunctionalInterface
public interface StreamCreated {
    void execute(QuicStreamChannel channel);

}
