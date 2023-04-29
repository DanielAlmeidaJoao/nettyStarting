package quicSupport.handlers.pipeline;

import io.netty.incubator.codec.quic.EarlyDataSendCallback;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.net.InetSocketAddress;

public class CustomEarlyDataSendCallback implements EarlyDataSendCallback {
    private static final Logger logger = LogManager.getLogger(CustomEarlyDataSendCallback.class);
    private final InetSocketAddress self;
    private final InetSocketAddress remote;
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;
    public CustomEarlyDataSendCallback(InetSocketAddress self, InetSocketAddress remote, CustomQuicChannelConsumer consumer, QuicChannelMetrics metrics){
        this.self = self;
        this.remote = remote;
        this.consumer = consumer;
        this.metrics = metrics;
    }
    @Override
    public void send(QuicChannel quicChannel) {
        logger.info("{} EARLY DATA TRIGGERED TO {}",self,remote);
        if(metrics!=null){
            metrics.initConnectionMetrics(quicChannel.remoteAddress());
        }
        try {
            QuicStreamChannel streamChannel = QUICLogics.createStream(quicChannel,consumer,metrics,false);
            QuicHandShakeMessage handShakeMessage = new QuicHandShakeMessage(self.getHostName(),self.getPort(),streamChannel.id().asShortText());
            byte [] hs = QUICLogics.gson.toJson(handShakeMessage).getBytes();
            streamChannel.writeAndFlush(QUICLogics.writeBytes(hs.length,hs, QUICLogics.HANDSHAKE_MESSAGE))
                    .addListener(future -> {
                        if(future.isSuccess()){
                            consumer.channelActive(streamChannel,null,remote);
                        }else{
                            logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,remote);
                            consumer.streamErrorHandler(streamChannel,future.cause());
                            future.cause().printStackTrace();
                            quicChannel.close();
                        }
                    });
            logger.debug("{} SENT CUSTOM HANDSHAKE DATA TO {}",self,remote);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void connectionFailed(QuicStreamChannel streamChannel, Future future,QuicChannel quicChannel){
        logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,remote);
        consumer.streamErrorHandler(streamChannel,future.cause());
        future.cause().printStackTrace();
        quicChannel.close();
    }
}
