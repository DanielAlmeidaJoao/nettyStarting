package quicSupport.handlers.funcHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;

@FunctionalInterface
public interface ConnectionInactive {
    void execute(String channelId, String streamId);
}
