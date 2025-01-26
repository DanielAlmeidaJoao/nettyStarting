package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.utils.funcs;

import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.metrics.UDPNetworkStatsWrapper;

import java.util.List;

@FunctionalInterface
public interface OnReadMetricsFunc {
    void execute(List<UDPNetworkStatsWrapper> stats);
}
