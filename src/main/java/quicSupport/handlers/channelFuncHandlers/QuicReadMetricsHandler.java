package quicSupport.handlers.channelFuncHandlers;

import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface QuicReadMetricsHandler {
    public void readMetrics(List<QuicConnectionMetrics> current, List<QuicConnectionMetrics> old);
}
