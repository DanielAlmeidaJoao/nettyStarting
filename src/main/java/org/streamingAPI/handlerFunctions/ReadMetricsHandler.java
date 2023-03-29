package org.streamingAPI.handlerFunctions;

import org.streamingAPI.metrics.TCPStreamConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<TCPStreamConnectionMetrics> current, List<TCPStreamConnectionMetrics> old);
}
