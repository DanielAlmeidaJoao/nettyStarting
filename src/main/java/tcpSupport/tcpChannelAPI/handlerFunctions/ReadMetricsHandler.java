package tcpSupport.tcpChannelAPI.handlerFunctions;

import tcpSupport.tcpChannelAPI.metrics.TCPStreamConnectionMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<TCPStreamConnectionMetrics> current, List<TCPStreamConnectionMetrics> old);
}
