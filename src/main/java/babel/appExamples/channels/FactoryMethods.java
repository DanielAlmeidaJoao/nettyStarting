package babel.appExamples.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.InetSocketAddress;

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

    public static Host toBabelHost(InetSocketAddress address){
        return new Host(address.getAddress(),address.getPort());
    }


    public static InetSocketAddress toInetSOcketAddress(Host address){
        return new InetSocketAddress(address.getAddress(),address.getPort());
    }
}
