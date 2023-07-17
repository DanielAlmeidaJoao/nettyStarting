package tcpSupport.tcpChannelAPI.handlerFunctions;

import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<ConnectionProtocolMetrics> current, List<ConnectionProtocolMetrics> old);
}
