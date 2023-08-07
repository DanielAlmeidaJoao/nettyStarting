package appExamples2.appExamples.channels.messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;

public class BytesToBabelMessage extends ProtoMessage {
    public static final short ID = 500;
    public final int dataLen;
    public final byte[] message;

    public BytesToBabelMessage(byte [] data, int len) {
        super(ID);
        this.dataLen = len;
        this.message = data;
    }

    public static ISerializer<BytesToBabelMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(BytesToBabelMessage bytesToBabelMessage, ByteBuf out) throws IOException {
            out.writeBytes(bytesToBabelMessage.message,0, bytesToBabelMessage.dataLen);
        }
        @Override
        public BytesToBabelMessage deserialize(ByteBuf in) {
            byte [] message = new byte[in.readableBytes()];
            in.readBytes(message);
            return new BytesToBabelMessage(message,message.length);
        }
    };
}
