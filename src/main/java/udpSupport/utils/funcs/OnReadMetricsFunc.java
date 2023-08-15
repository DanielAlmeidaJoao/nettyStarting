package udpSupport.utils.funcs;

import udpSupport.metrics.UDPNetworkStatsWrapper;

import java.util.List;

@FunctionalInterface
public interface OnReadMetricsFunc {
    void execute(List<UDPNetworkStatsWrapper> stats);
}
