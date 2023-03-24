package quicSupport.utils;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.quic.*;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.handlers.pipeline.ServerChannelInitializer;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Logics {
    public static final int WRT_OFFSET=5; //4 BYTES(DATA LEN)+ 1 BYTE (MESSAGE CODE)
    public final static byte HANDSHAKE_MESSAGE = 'A';
    public final static byte APP_DATA = 'B';
    public static final String HOST_NAME = "HOST";
    public static final String PORT = "PORT";
    public static final Gson gson = new Gson();

    private static final long maxIdleTimeoutInSeconds=30;
    private static final long initialMaxData=10000000;
    private static final long initialMaxStreamDataBidirectionalLocal=1000000;
    private static final long initialMaxStreamDataBidirectionalRemote=1000000;
    private static final long initialMaxStreamsBidirectional=10;
    private static final long initialMaxStreamsUnidirectional=10;

    private static final long maxAckDelay = 100;

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
    public static QuicCodecBuilder addConfigs(QuicCodecBuilder codecBuilder, Properties properties){
        return codecBuilder
                .maxIdleTimeout((Long) properties.getOrDefault("maxIdleTimeoutInSeconds",maxIdleTimeoutInSeconds), TimeUnit.SECONDS)
                .initialMaxData((Long) properties.getOrDefault("initialMaxData",initialMaxData))
                .initialMaxStreamDataBidirectionalLocal((Long) properties.getOrDefault("initialMaxStreamDataBidirectionalLocal",initialMaxStreamDataBidirectionalLocal))
                .initialMaxStreamDataBidirectionalRemote((Long) properties.getOrDefault("initialMaxStreamDataBidirectionalRemote",initialMaxStreamDataBidirectionalRemote))
                .initialMaxStreamsBidirectional((Long) properties.getOrDefault("initialMaxStreamsBidirectional",initialMaxStreamsBidirectional))
                .initialMaxStreamsUnidirectional((Long) properties.getOrDefault("initialMaxStreamsUnidirectional",initialMaxStreamsUnidirectional))
                .maxAckDelay((Long) properties.getOrDefault("maxAckDelay",maxAckDelay), TimeUnit.MILLISECONDS)
                .activeMigration(true);
    }
}
