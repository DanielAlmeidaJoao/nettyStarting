package appExamples2.appExamples.channels.udpBabelChannel;

import lombok.Getter;
import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import udpSupport.metrics.ChannelStats;

@Getter
public class UDPMetricsEvent extends ChannelEvent {
    private ChannelStats stats;
    public static final short EVENT_ID = 13;

    public UDPMetricsEvent(ChannelStats stats) {
        super(EVENT_ID);
        this.stats=stats;
    }
}
