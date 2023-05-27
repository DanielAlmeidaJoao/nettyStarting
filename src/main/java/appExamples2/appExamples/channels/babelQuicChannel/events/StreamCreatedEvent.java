package appExamples2.appExamples.channels.babelQuicChannel.events;

import pt.unl.fct.di.novasys.babel.channels.ChannelEvent;
import pt.unl.fct.di.novasys.babel.channels.Host;
import quicSupport.utils.enums.ConnectionOrStreamType;

public class StreamCreatedEvent extends ChannelEvent {
    public static final short EVENT_ID = 14;
    public final String streamId;
    public final Host host;
    public final ConnectionOrStreamType connectionOrStreamType;
    public StreamCreatedEvent(String streamId, Host peer,ConnectionOrStreamType type) {
        super(EVENT_ID);
        this.streamId = streamId;
        this.connectionOrStreamType=type;
        host = peer;
    }
}
