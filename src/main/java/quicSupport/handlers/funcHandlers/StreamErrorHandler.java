package quicSupport.handlers.funcHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

@FunctionalInterface
public interface StreamErrorHandler {
    void execute(QuicStreamChannel channel, Throwable cause);

}
