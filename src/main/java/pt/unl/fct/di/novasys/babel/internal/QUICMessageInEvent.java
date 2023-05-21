package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class QUICMessageInEvent extends InternalEvent {

    private final BabelMessage deliverMessageInMsg;
    private final Host from;
    private final int channelId;
    public final String streamId;

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public QUICMessageInEvent(BabelMessage msg, Host from, int channelId, String streamId) {
        super(EventType.QUIC_MESSAGE_IN_EVENT);
        this.from = from;
        this.deliverMessageInMsg = msg;
        this.channelId = channelId;
        this.streamId =streamId;
    }

    @Override
    public String toString() {
        return "MessageInEvent{" +
                "msg=" + deliverMessageInMsg +
                ", from=" + from +
                ", channelId=" + channelId +
                '}';
    }

    public final Host getFrom() {
        return this.from;
    }

    public int getChannelId() {
        return channelId;
    }

    public BabelMessage getMsg() {
        return deliverMessageInMsg;
    }

}
