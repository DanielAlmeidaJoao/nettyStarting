package appExamples2.appExamples.channels.babelQuicChannel.events;

import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import pt.unl.fct.di.novasys.babel.channels.Host;

public class StreamClosedEvent extends ChannelEvent {
    public static final short EVENT_ID = 13;
    public final Host host;
    public final String streamId;

    public StreamClosedEvent(String streamId, Host host) {
        super(EVENT_ID);
        this.host= host;
        this.streamId=streamId;
    }
}
