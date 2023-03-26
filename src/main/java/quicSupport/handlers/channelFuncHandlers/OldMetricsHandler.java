package quicSupport.handlers.channelFuncHandlers;

import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.net.InetSocketAddress;
import java.util.List;

@FunctionalInterface
public interface OldMetricsHandler {
    void handle(List<QuicConnectionMetrics> oldMetrics);

}
