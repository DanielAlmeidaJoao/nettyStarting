package babel.appExamples.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;

public class FactoryMethods {

    public static <T> byte [] toSend(ISerializer<T> serializer, T msg) throws IOException {
        ByteBuf out = Unpooled.buffer();
        serializer.serialize(msg, out);
        byte [] toSend = new byte[out.readableBytes()];
        out.readBytes(toSend);
        out.release();
        return toSend;
    }

    public static <T> T unSerialize(ISerializer<T> serializer, byte[] bytes) throws IOException {
        ByteBuf in = Unpooled.copiedBuffer(bytes);
        T payload = serializer.deserialize(in);
        in.release();
        return payload;
    }
}
