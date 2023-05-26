package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class BytesMessageInEvent extends InternalEvent {

    private final byte [] deliverMessageInMsg;
    private final Host from;
    private final int channelId;
    public final String streamId;
    public final short sourceProto;
    public final short destProto;
    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public BytesMessageInEvent(byte [] msg, Host from, int channelId, String streamId,short sourceProto, short destProto) {
        super(EventType.BYTE_MESSAGE_IN);
        this.from = from;
        this.deliverMessageInMsg = msg;
        this.channelId = channelId;
        this.streamId =streamId;
        this.sourceProto = sourceProto;
        this.destProto=destProto;
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

    public byte [] getMsg() {
        return deliverMessageInMsg;
    }

}
