package quicSupport.utils;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import quicSupport.handlers.QuicDelimitedMessageDecoder;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.handlers.server.QuicStreamReadHandler;

public class Logics {

    public static final String HOST_NAME = "HOST";
    public static final String PORT = "PORT";
    public static final Gson gson = new Gson();

    public static QuicStreamChannel createStream(QuicChannel quicChan, QuicListenerExecutor quicListenerExecutor) throws Exception{
        QuicStreamChannel streamChannel = quicChan
                .createStream(QuicStreamType.BIDIRECTIONAL,new ChannelInitializer<QuicStreamChannel>() {
                    @Override
                    protected void initChannel(QuicStreamChannel channel) throws Exception {
                        channel.pipeline().addLast(new QuicDelimitedMessageDecoder(quicListenerExecutor));
                        channel.pipeline().addLast(new QuicStreamReadHandler(quicListenerExecutor));
                    }
                })
                .sync()
                .getNow();
        return streamChannel;
    }
    public static ByteBuf writeBytes(int len, byte [] data, byte msgType){
        ByteBuf buf = Unpooled.buffer(len+5);
        buf.writeInt(len);
        buf.writeByte(msgType);
        buf.writeBytes(data,0,len);
        return buf;
    }
}
