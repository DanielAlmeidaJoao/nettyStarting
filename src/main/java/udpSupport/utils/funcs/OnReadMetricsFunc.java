package udpSupport.utils.funcs;

import udpSupport.metrics.NetworkStatsWrapper;

import java.util.List;

@FunctionalInterface
public interface OnReadMetricsFunc {
    void execute(List<NetworkStatsWrapper> stats);
}
