package appExamples2.appExamples.channels;

import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.babel.channels.ChannelListener;
import pt.unl.fct.di.novasys.babel.channels.Host;
import pt.unl.fct.di.novasys.babel.channels.ISerializer;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.ConnectionOrStreamType;

import java.io.IOException;
import java.net.InetSocketAddress;

public class FactoryMethods {

    public final static String SINGLE_THREADED_PROP="SINGLE_THREADED";
    public final static short BYTE_MESSAGE_ID = -1;

    public static <T> byte [] toSend(ISerializer<T> serializer, T msg) throws IOException {
        ByteBuf out = Unpooled.buffer();
        serializer.serialize(msg, out);
        byte [] toSend = new byte[out.readableBytes()];
        out.readBytes(toSend);
        out.release();
        return toSend;
    }

    public static <T> T unSerialize(ISerializer<T> serializer, byte[] bytes,ConnectionOrStreamType type,short protoToReceiveStreamData) throws IOException {
        if(ConnectionOrStreamType.UNSTRUCTURED_STREAM==type){
            return (T) new BabelMessage(new BytesMessageSentOrFail(protoToReceiveStreamData,bytes,bytes.length)
                    ,protoToReceiveStreamData,protoToReceiveStreamData);
        }else {
            ByteBuf in = Unpooled.copiedBuffer(bytes);
            T payload = serializer.deserialize(in);
            in.release();
            return payload;
        }
    }

    public static Host toBabelHost(InetSocketAddress address){
        return new Host(address.getAddress(),address.getPort());
    }


    public static InetSocketAddress toInetSOcketAddress(Host address){
        return new InetSocketAddress(address.getAddress(),address.getPort());
    }
    public static byte [] serializeWhenSendingBytes(short sourceProto, short destProto,short handlerId, byte [] data, int dataLen){
        ByteBuf byteBuf = Unpooled.buffer(8+dataLen);
        byteBuf.writeShort(sourceProto);
        byteBuf.writeShort(destProto);
        byteBuf.writeShort(BYTE_MESSAGE_ID);
        byteBuf.writeShort(handlerId);
        byteBuf.writeBytes(data,0,dataLen);
        byte [] outPut = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(outPut);
        byteBuf.release();
        return outPut;
    }
    public static void deserialize(byte [] data, BabelMessageSerializerInterface serializer, ChannelListener listener, InetSocketAddress from, String streamId)
            throws IOException{
        ByteBuf byteBuf = Unpooled.copiedBuffer(data);
        short sourceProto = byteBuf.readShort();
        short destProto = byteBuf.readShort();
        short msgId = byteBuf.readShort();
        if(msgId==BYTE_MESSAGE_ID){
            short handlerId = byteBuf.readShort();
            data = new byte [byteBuf.readableBytes()];
            byteBuf.readBytes(data);
            listener.deliverMessage(data,FactoryMethods.toBabelHost(from),streamId,sourceProto,destProto,handlerId);
        }else{
            ISerializer<? extends ProtoMessage> iSerializer = serializer.getSerializer(msgId);
            if(iSerializer == null){
                throw new AssertionError("No deserializer found for message id " + msgId);
            }
            ProtoMessage deserialize = iSerializer.deserialize(byteBuf);
            listener.deliverMessage(new BabelMessage(deserialize, sourceProto, destProto),FactoryMethods.toBabelHost(from),streamId);
        }
        byteBuf.release();
    }
}
