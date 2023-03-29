package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.Logics;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.net.InetSocketAddress;

public class QuicClientChannelConHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QuicClientChannelConHandler.class);
    private final InetSocketAddress self;
    private final InetSocketAddress remote;
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;

    public QuicClientChannelConHandler(InetSocketAddress self, InetSocketAddress remote, CustomQuicChannelConsumer consumer, QuicChannelMetrics  metrics) {
        this.self = self;
        this.remote = remote;
        this.consumer = consumer;
        this.metrics = metrics;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        QuicChannel out = (QuicChannel) ctx.channel();
        System.out.println(out.config());
        logger.info("CLIENT CHANNEL ACTIVE!!!");
        if(metrics!=null){
            metrics.initConnectionMetrics(out.remoteAddress());
        }
        logger.info("{} ESTABLISHED CONNECTION WITH {}",self,remote);
        HandShakeMessage handShakeMessage = new HandShakeMessage(self.getHostName(),self.getPort());
        byte [] hs = Logics.gson.toJson(handShakeMessage).getBytes();
        QuicStreamChannel streamChannel = Logics.createStream(out,consumer,metrics,false);
        streamChannel.writeAndFlush(Logics.writeBytes(hs.length,hs, Logics.HANDSHAKE_MESSAGE))
                .addListener(future -> {
                    if(future.isSuccess()){
                        consumer.channelActive(streamChannel,null,remote);
                    }else{
                        logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,remote);
                        consumer.streamErrorHandler(streamChannel,future.cause());
                        future.cause().printStackTrace();
                        out.close();
                    }
                });
        logger.info("{} SENT CUSTOM HANDSHAKE DATA TO {}",self,remote);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.onConnectionClosed(ctx.channel().remoteAddress());
        }
        consumer.channelInactive(ctx.channel().id().asShortText());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        consumer.onOpenConnectionFailed((InetSocketAddress) ctx.channel().remoteAddress(),cause);
        cause.printStackTrace();
    }
}
