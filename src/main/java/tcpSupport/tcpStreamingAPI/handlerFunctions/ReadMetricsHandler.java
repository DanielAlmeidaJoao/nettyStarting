package tcpSupport.tcpStreamingAPI.handlerFunctions;

import tcpSupport.tcpStreamingAPI.metrics.TCPSConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<TCPSConnectionMetrics> current, List<TCPSConnectionMetrics> old);
}
