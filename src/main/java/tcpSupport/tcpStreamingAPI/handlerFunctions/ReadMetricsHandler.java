package tcpSupport.tcpStreamingAPI.handlerFunctions;

import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<TCPStreamConnectionMetrics> current, List<TCPStreamConnectionMetrics> old);
}
