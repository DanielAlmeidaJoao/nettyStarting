package quicSupport.handlers.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.net.InetSocketAddress;

public class QuicClientChannelConHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LogManager.getLogger(QuicClientChannelConHandler.class);
    private final InetSocketAddress self;
    private final ConnectionId identification;
    private final CustomQuicChannelConsumer consumer;
    private final QuicChannelMetrics metrics;
    private final TransmissionType transmissionType;

    public QuicClientChannelConHandler(InetSocketAddress self, ConnectionId identification, CustomQuicChannelConsumer consumer, QuicChannelMetrics  metrics, TransmissionType transmissionType) {
        this.self = self;
        this.identification = identification;
        this.consumer = consumer;
        this.metrics = metrics;
        this.transmissionType = transmissionType;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("{} ESTABLISHED CONNECTION WITH {}",self,identification);
        QuicChannel out = (QuicChannel) ctx.channel();
        if(metrics!=null){
            metrics.initConnectionMetrics(out.remoteAddress());
        }
        QuicStreamChannel streamChannel = QUICLogics.createStream(out,consumer,metrics,identification);
        QuicHandShakeMessage handShakeMessage = new QuicHandShakeMessage(self.getHostName(),self.getPort(),streamChannel.id().asShortText(), transmissionType);
        byte [] hs = QUICLogics.gson.toJson(handShakeMessage).getBytes();
        streamChannel.writeAndFlush(QUICLogics.writeBytes(hs.length,hs,QUICLogics.HANDSHAKE_MESSAGE, TransmissionType.STRUCTURED_MESSAGE))
                .addListener(future -> {
                    if(future.isSuccess()){
                        consumer.channelActive(streamChannel,null,identification, transmissionType);
                    }else{
                        logger.info("{} CONNECTION TO {} COULD NOT BE ACTIVATED.",self,identification);
                        consumer.streamErrorHandler(identification,future.cause());
                        future.cause().printStackTrace();
                        out.close();
                    }
                });
        logger.debug("{} SENT CUSTOM HANDSHAKE DATA TO {}",self,identification);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(metrics!=null){
            metrics.onConnectionClosed(ctx.channel().remoteAddress());
        }
        consumer.channelInactive(identification);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        consumer.handleOpenConnectionFailed(identification,cause);
        cause.printStackTrace();
    }
}
