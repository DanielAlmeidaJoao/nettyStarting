package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import tcpSupport.tcpChannelAPI.utils.BabelInputStream;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

/**
 * An abstract class that represents a protocol message
 *
 * @see InternalEvent
 * @see GenericProtocol
 */
public class BabelStreamDeliveryEvent extends InternalEvent {

    public final BabelOutputStream babelOutputStream;
    private final Host from;
    private final int channelId;
    public final String conId;
    public final short sourceProto;
    public final short destProto;
    public final short handlerId;
    public final BabelInputStream babelInputStream;
    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public BabelStreamDeliveryEvent(BabelOutputStream networkDataCarrier, Host from, int channelId, String conId, short sourceProto, short destProto, short handlerId, BabelInputStream inputStream) {
        super(EventType.STREAM_BYTES_IN);
        this.from = from;
        this.babelOutputStream = networkDataCarrier;
        this.channelId = channelId;
        this.conId = conId;
        this.sourceProto = sourceProto;
        this.destProto=destProto;
        this.handlerId=handlerId;
        this.babelInputStream = inputStream;
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
