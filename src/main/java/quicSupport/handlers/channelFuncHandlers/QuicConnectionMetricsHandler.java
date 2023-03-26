package quicSupport.handlers.channelFuncHandlers;

import quicSupport.utils.entities.QuicConnectionMetrics;

import java.net.InetSocketAddress;

@FunctionalInterface
public interface QuicConnectionMetricsHandler {
    void handle(InetSocketAddress peer, QuicConnectionMetrics metrics);

}
