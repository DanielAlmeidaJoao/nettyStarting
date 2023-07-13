package pt.unl.fct.di.novasys.babel.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import pt.unl.fct.di.novasys.network.data.Host;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;

import java.util.LinkedList;
import java.util.List;

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
    public final String conId;
    public final short sourceProto;
    public final short destProto;
    public final short handlerId;
    /**
     * Create a protocol message event with the provided numeric identifier
     */
    public BytesMessageInEvent(byte [] msg, Host from, int channelId, String conId, short sourceProto, short destProto, short handlerId) {
        super(EventType.BYTE_MESSAGE_IN);
        this.from = from;
        this.deliverMessageInMsg = msg;
        this.channelId = channelId;
        this.conId = conId;
        this.sourceProto = sourceProto;
        this.destProto=destProto;
        this.handlerId=handlerId;
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

    public List<Integer> readDataAsInteger(){
        List<Integer> integerList = new LinkedList<>();
        ByteBuf buf = Unpooled.copiedBuffer(deliverMessageInMsg);
        while (buf.readableBytes()>=4){
            integerList.add(buf.readInt());
        }
        buf.release();
        return integerList;
    }
    public List<Float> readDataAsFloat(){
        List<Float> integerList = new LinkedList<>();
        ByteBuf buf = Unpooled.copiedBuffer(deliverMessageInMsg);
        int bytes = Float.BYTES;
        while (buf.readableBytes()>=bytes){
            integerList.add(buf.readFloat());
        }
        buf.release();
        return integerList;
    }
    public List<Long> readDataAsLong(){
        List<Long> integerList = new LinkedList<>();
        ByteBuf buf = Unpooled.copiedBuffer(deliverMessageInMsg);
        int bytes = Long.BYTES;
        while (buf.readableBytes()>=bytes){
            integerList.add(buf.readLong());
        }
        buf.release();
        return integerList;
    }
    public List<Double> readDataAsDouble(){
        List<Double> integerList = new LinkedList<>();
        ByteBuf buf = Unpooled.copiedBuffer(deliverMessageInMsg);
        int bytes = Double.BYTES;
        while (buf.readableBytes()>=bytes){
            integerList.add(buf.readDouble());
        }
        buf.release();
        return integerList;
    }
    public List<Short> readDataAsShort(){
        List<Short> integerList = new LinkedList<>();
        ByteBuf buf = Unpooled.copiedBuffer(deliverMessageInMsg);
        int bytes = Short.BYTES;
        while (buf.readableBytes()>=bytes){
            integerList.add(buf.readShort());
        }
        buf.release();
        return integerList;
    }
    public List<Boolean> readDataAsBoolean(){
        List<Boolean> integerList = new LinkedList<>();
        ByteBuf buf = Unpooled.copiedBuffer(deliverMessageInMsg);
        int bytes = 1;
        while (buf.readableBytes()>=bytes){
            integerList.add(buf.readBoolean());
        }
        buf.release();
        return integerList;
    }

}
