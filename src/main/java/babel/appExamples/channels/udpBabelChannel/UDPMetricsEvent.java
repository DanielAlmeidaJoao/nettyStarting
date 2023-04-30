package babel.appExamples.channels.udpBabelChannel;

import lombok.Getter;
import pt.unl.fct.di.novasys.channel.ChannelEvent;
import quicSupport.utils.metrics.QuicConnectionMetrics;
import udpSupport.metrics.ChannelStats;

import java.util.List;

@Getter
public class UDPMetricsEvent extends ChannelEvent {
    private ChannelStats stats;
    public static final short EVENT_ID = 13;

    public UDPMetricsEvent(ChannelStats stats) {
        super(EVENT_ID);
        this.stats=stats;
    }
}
