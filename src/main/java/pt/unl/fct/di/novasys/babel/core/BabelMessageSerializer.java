package pt.unl.fct.di.novasys.babel.core;

import appExamples2.appExamples.channels.FactoryMethods;
import appExamples2.appExamples.channels.babelQuicChannel.BytesMessageSentOrFail;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

import java.io.IOException;
import java.util.Map;

public class BabelMessageSerializer implements BabelMessageSerializerInterface<BabelMessage> {

    Map<Short, ISerializer<? extends ProtoMessage>> serializers;

    public ISerializer<? extends  ProtoMessage> getSerializer(short serializerId){
        return serializers.get(serializerId);
    }
    public BabelMessageSerializer(Map<Short, ISerializer<? extends ProtoMessage>> serializers) {
        this.serializers = serializers;
    }

    public void registerProtoSerializer(short msgCode, ISerializer<? extends ProtoMessage> protoSerializer) {
        if (serializers.putIfAbsent(msgCode, protoSerializer) != null)
            throw new AssertionError("Trying to re-register serializer in Babel: " + msgCode);
    }

    @Override
    public void serialize(BabelMessage msg, ByteBuf byteBuf) throws IOException {
        byteBuf.writeShort(msg.getSourceProto());
        byteBuf.writeShort(msg.getDestProto());
        byteBuf.writeShort(msg.getMessage().getId());
        ISerializer iSerializer = serializers.get(msg.getMessage().getId());
        if(iSerializer == null){
            throw new AssertionError("No serializer found for message id " + msg.getMessage().getId());
        }
        iSerializer.serialize(msg.getMessage(), byteBuf);
    }

    @Override
    public BabelMessage deserialize(ByteBuf byteBuf) throws IOException {
        short source = byteBuf.readShort();
        short dest = byteBuf.readShort();
        short id = byteBuf.readShort();
        ISerializer<? extends ProtoMessage> iSerializer = serializers.get(id);
        if(iSerializer == null ){
            if(id == FactoryMethods.BYTE_MESSAGE_ID){
                short handlerId = byteBuf.readShort();
                byte [] data = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(data);

                return new BabelMessage(new BytesMessageSentOrFail(handlerId,data,null,data.length), source, dest);
            }
            throw new AssertionError("No deserializer found for message id " + id);
        }
        ProtoMessage deserialize = iSerializer.deserialize(byteBuf);
        return new BabelMessage(deserialize, source, dest);
    }
}