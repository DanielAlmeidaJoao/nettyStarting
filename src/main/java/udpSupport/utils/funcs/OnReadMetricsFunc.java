package udpSupport.utils.funcs;

import udpSupport.metrics.ChannelStats;

@FunctionalInterface
public interface OnReadMetricsFunc {
    void execute(ChannelStats stats);
}
