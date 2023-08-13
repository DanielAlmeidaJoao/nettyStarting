package appExamples2.appExamples.channels.babelNewChannels.events;

import lombok.Getter;
import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;

import java.util.List;

@Getter
public class QUICMetricsEvent extends ChannelEvent {
    private List<ConnectionProtocolMetrics> current;
    private List<ConnectionProtocolMetrics> old;
    public static final short EVENT_ID = 12;

    public QUICMetricsEvent(List<ConnectionProtocolMetrics> current,List<ConnectionProtocolMetrics> old) {
        super(EVENT_ID);
        this.current=current;
        this.old=old;
    }
}
