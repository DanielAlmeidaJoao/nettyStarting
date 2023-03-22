package quicSupport.utils;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.quic.*;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.handlers.pipeline.ServerChannelInitializer;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.util.concurrent.TimeUnit;

import static quicSupport.client_server.QuicClientExample.DEFAULT_IDLE_TIMEOUT;

public class Logics {
    public static final int WRT_OFFSET=5; //4 BYTES(DATA LEN)+ 1 BYTE (MESSAGE CODE)
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    public static final String HOST_NAME = "HOST";
    public static final String PORT = "PORT";
    public static final Gson gson = new Gson();

    public static QuicStreamChannel createStream(QuicChannel quicChan, QuicListenerExecutor quicListenerExecutor, QuicChannelMetrics metrics) throws Exception{
        QuicStreamChannel streamChannel = quicChan
                .createStream(QuicStreamType.BIDIRECTIONAL,new ServerChannelInitializer(quicListenerExecutor,metrics))
                .addListener(future -> {
                    if(metrics!=null && future.isSuccess()){
                        QuicConnectionMetrics q = metrics.getConnectionMetrics(quicChan.remoteAddress());
                        q.setCreatedStreamCount(q.getCreatedStreamCount()+1);
                    }
                })
                .sync()
                .getNow();
        QuicStreamChannelConfig config = streamChannel.config();
        config.setAllowHalfClosure(false);
        return streamChannel;
    }
    public static ByteBuf writeBytes(int len, byte [] data, byte msgType){
        ByteBuf buf = Unpooled.buffer(len+WRT_OFFSET);
        buf.writeInt(len);
        buf.writeByte(msgType);
        buf.writeBytes(data,0,len);
        return buf;
    }
    public static QuicCodecBuilder addConfigs(QuicCodecBuilder codecBuilder, long maxIdleTimeoutInSeconds,
                                       long initialMaxData, long initialMaxStreamDataBidirectionalLocal,
                                       long initialMaxStreamDataBidirectionalRemote,
                                       long initialMaxStreamsBidirectional,
                                       long initialMaxStreamsUnidirectional){
        return codecBuilder
                .maxIdleTimeout(maxIdleTimeoutInSeconds, TimeUnit.SECONDS)
                .initialMaxData(initialMaxData)
                .initialMaxStreamDataBidirectionalLocal(initialMaxStreamDataBidirectionalLocal)
                .initialMaxStreamDataBidirectionalRemote(initialMaxStreamDataBidirectionalRemote)
                .initialMaxStreamsBidirectional(initialMaxStreamsBidirectional)
                .initialMaxStreamsUnidirectional(initialMaxStreamsUnidirectional);
    }
}
