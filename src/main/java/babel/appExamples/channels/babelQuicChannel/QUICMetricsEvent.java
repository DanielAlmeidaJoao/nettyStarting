package babel.appExamples.channels.babelQuicChannel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pt.unl.fct.di.novasys.channel.ChannelEvent;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.util.List;

@Getter
public class QUICMetricsEvent extends ChannelEvent {
    private List<QuicConnectionMetrics> current;
    private List<QuicConnectionMetrics> old;
    public static final short EVENT_ID = 12;

    public QUICMetricsEvent(List<QuicConnectionMetrics> current,List<QuicConnectionMetrics> old) {
        super(EVENT_ID);
        this.current=current;
        this.old=old;
    }
}
