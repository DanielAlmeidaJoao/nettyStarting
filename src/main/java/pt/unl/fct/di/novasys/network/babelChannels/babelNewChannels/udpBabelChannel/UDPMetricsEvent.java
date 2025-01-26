package pt.unl.fct.di.novasys.network.babelChannels.babelNewChannels.udpBabelChannel;

import lombok.Getter;
import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.udpSupport.metrics.UDPNetworkStatsWrapper;

import java.util.List;

@Getter
public class UDPMetricsEvent extends ChannelEvent {
    public final List<UDPNetworkStatsWrapper> stats;
    public static final short EVENT_ID = 13;

    public UDPMetricsEvent(List<UDPNetworkStatsWrapper> stats) {
        super(EVENT_ID);
        this.stats=stats;
    }
}
