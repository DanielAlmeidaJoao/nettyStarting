package org.tcpStreamingAPI.handlerFunctions;

import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<TCPStreamConnectionMetrics> current, List<TCPStreamConnectionMetrics> old);
}
