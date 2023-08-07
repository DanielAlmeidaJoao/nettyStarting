package appExamples2.appExamples.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

public class FactoryMethods {

    public final static String SINGLE_THREADED_PROP="SINGLE_THREADED";

    public static <T> ByteBuf toSend(ISerializer<T> serializer, T msg) throws IOException {
        ByteBuf out = Unpooled.buffer();
        serializer.serialize(msg, out);
        /*
        byte [] toSend = new byte[out.readableBytes()];
        out.readBytes(toSend);
        out.release();
        */
        return out;
    }
    private static int available(InputStream inputStream){
        //new Exception("TO DO: MAKE unSerialize RECEIVE nr BYTES SEMT").printStackTrace();
        try{
            return inputStream.available();
        }catch (Exception e){};
        return 0;
    }


    public static Host toBabelHost(InetSocketAddress address){
        return new Host(address.getAddress(),address.getPort());
    }


    public static InetSocketAddress toInetSOcketAddress(Host address){
        return new InetSocketAddress(address.getAddress(),address.getPort());
    }

}
