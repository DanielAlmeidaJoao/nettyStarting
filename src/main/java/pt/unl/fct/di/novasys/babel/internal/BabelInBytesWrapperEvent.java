package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class BabelInBytesWrapperEvent extends InternalEvent {

    public final BabelOutputStream babelOutputStream;
    private final Host from;
    private final int channelId;
    public final String conId;
    public final short sourceProto;
    public final short destProto;
    public final short handlerId;
    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public BabelInBytesWrapperEvent(BabelOutputStream wrapper, Host from, int channelId, String conId, short sourceProto, short destProto, short handlerId) {
        super(EventType.STREAM_BYTES_IN);
        this.from = from;
        this.babelOutputStream = wrapper;
        this.channelId = channelId;
        this.conId = conId;
        this.sourceProto = sourceProto;
        this.destProto=destProto;
        this.handlerId=handlerId;
    }

    @Override
    public String toString() {
        return "MessageInEvent{" +
                "msg=" + babelOutputStream +
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

    public BabelOutputStream getBabelOutputStream() {
        return babelOutputStream;
    }


}
