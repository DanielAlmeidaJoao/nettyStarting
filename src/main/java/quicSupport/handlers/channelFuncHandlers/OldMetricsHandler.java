package quicSupport.handlers.channelFuncHandlers;

import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface OldMetricsHandler {
    void handle(List<QuicConnectionMetrics> oldMetrics);

}
