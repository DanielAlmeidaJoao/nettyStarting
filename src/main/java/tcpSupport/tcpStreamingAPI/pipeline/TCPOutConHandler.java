package tcpSupport.tcpStreamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.tuple.Pair;
import tcpSupport.tcpStreamingAPI.channel.TCPNettyConsumer;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.metrics.TCPSConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPMetrics;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

//@ChannelHandler.Sharable
public class TCPOutConHandler extends CustomChannelHandler {
    private HandShakeMessage handshakeData;
    private final TCPMetrics metrics;
    private final TransmissionType type;
    public final Pair<InetSocketAddress,String> connectionId;

    public TCPOutConHandler(HandShakeMessage handshakeData, TCPNettyConsumer consumer, TCPMetrics metrics, TransmissionType type, Pair connectionId){
        super(consumer,connectionId);
        this.handshakeData = handshakeData;
        this.metrics = metrics;
        this.type = type;
        this.connectionId = connectionId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws UnknownHostException {
        byte [] data = TCPStreamUtils.g.toJson(handshakeData).getBytes();
        ByteBuf tmp = Unpooled.buffer(data.length+4);
        tmp.writeInt(data.length);
        tmp.writeBytes(data);
        if(metrics!=null){
            metrics.initConnectionMetrics(ctx.channel().remoteAddress());
        }
        ctx.writeAndFlush(tmp).addListener(future -> {
            if(future.isSuccess()){
                if(metrics!=null){
                    TCPSConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
                    metrics1.setSentControlBytes(metrics1.getSentControlBytes()+data.length+4);
                    metrics1.setSentControlMessages(metrics1.getSentControlMessages()+1);
                }
            }else{
                future.cause().printStackTrace();
                ctx.channel().close();
            }
        });
        getConsumer().onChannelActive(ctx.channel(),null,type,connectionId);
        handshakeData=null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}