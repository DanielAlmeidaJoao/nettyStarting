package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class MessageInEvent extends InternalEvent {

    private final pt.unl.fct.di.novasys.babel.internal.BabelMessage deliverMessageInMsg;
    private final Host from;
    private final int channelId;
    public final String connectionId;

    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public MessageInEvent(pt.unl.fct.di.novasys.babel.internal.BabelMessage msg, Host from, int channelId,String connectionId) {
        super(EventType.MESSAGE_IN_EVENT);
        this.from = from;
        this.deliverMessageInMsg = msg;
        this.channelId = channelId;
        this.connectionId = connectionId;
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
