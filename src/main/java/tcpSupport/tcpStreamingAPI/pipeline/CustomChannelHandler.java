package tcpSupport.tcpStreamingAPI.pipeline;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tcpSupport.tcpStreamingAPI.channel.TCPNettyConsumer;

import java.net.InetSocketAddress;

//@ChannelHandler.Sharable
public abstract class CustomChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LogManager.getLogger(CustomChannelHandler.class);
    @Getter
    private final TCPNettyConsumer consumer;
    public final Pair<InetSocketAddress,String> connectionId;

    public CustomChannelHandler(TCPNettyConsumer consumer, Pair<InetSocketAddress,String> connectionId){
        this.consumer = consumer;
        this.connectionId = connectionId;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx){
        consumer.onChannelInactive(connectionId);
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        consumer.onConnectionFailed(connectionId,cause);
        cause.printStackTrace();
        ctx.close();
        logger.error(cause.getLocalizedMessage());
    }
}
