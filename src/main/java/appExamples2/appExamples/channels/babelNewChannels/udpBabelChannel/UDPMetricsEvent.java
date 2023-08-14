package appExamples2.appExamples.channels.babelNewChannels.udpBabelChannel;

import lombok.Getter;
import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import udpSupport.metrics.NetworkStatsWrapper;

import java.util.List;

@Getter
public class UDPMetricsEvent extends ChannelEvent {
    public final List<NetworkStatsWrapper> stats;
    public static final short EVENT_ID = 13;

    public UDPMetricsEvent(List<NetworkStatsWrapper> stats) {
        super(EVENT_ID);
        this.stats=stats;
    }
}
