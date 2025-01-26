package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.handlerFunctions;

import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyTCPChannel.metrics.ConnectionProtocolMetrics;

import java.util.List;

@FunctionalInterface
public interface ReadMetricsHandler {
    public void readMetrics(List<ConnectionProtocolMetrics> current, List<ConnectionProtocolMetrics> old);
}
