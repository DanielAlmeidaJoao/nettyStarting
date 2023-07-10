package tcpSupport.tcpStreamingAPI.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import tcpSupport.tcpStreamingAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.TCPStreamMessageDecoder;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.TCPDelimitedMessageDecoder;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;
import quicSupport.utils.enums.TransmissionType;

import java.net.UnknownHostException;

//@ChannelHandler.Sharable
public class StreamSenderHandler extends CustomChannelHandler {
    private HandShakeMessage handshakeData;
    private final TCPStreamMetrics metrics;
    private final TransmissionType type;

    public StreamSenderHandler(HandShakeMessage handshakeData, StreamingNettyConsumer consumer, TCPStreamMetrics metrics, TransmissionType type){
        super(consumer);
        this.handshakeData = handshakeData;
        this.metrics = metrics;
        this.type = type;
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
                    TCPStreamConnectionMetrics metrics1 = metrics.getConnectionMetrics(ctx.channel().remoteAddress());
                    metrics1.setSentControlBytes(metrics1.getSentControlBytes()+data.length+4);
                    metrics1.setSentControlMessages(metrics1.getSentControlMessages()+1);
                }
            }else{
                future.cause().printStackTrace();
                ctx.channel().close();
            }
        });
        if(TransmissionType.UNSTRUCTURED_STREAM == type){
            ctx.channel().pipeline().replace(TCPDelimitedMessageDecoder.NAME, TCPStreamMessageDecoder.NAME,new TCPStreamMessageDecoder(metrics,getConsumer()));
        }
        getConsumer().onChannelActive(ctx.channel(),null,type);
        handshakeData=null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}