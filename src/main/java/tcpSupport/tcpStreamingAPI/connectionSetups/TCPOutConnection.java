package tcpSupport.tcpStreamingAPI.connectionSetups;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.lang3.tuple.Pair;
import tcpSupport.tcpStreamingAPI.channel.TCPNettyConsumer;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.metrics.TCPMetrics;
import tcpSupport.tcpStreamingAPI.pipeline.TCPOutConHandler;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.DelimitedMessageDecoder;
import tcpSupport.tcpStreamingAPI.pipeline.encodings.StreamMessageDecoder;
import quicSupport.utils.enums.TransmissionType;

import java.net.InetSocketAddress;
public class TCPOutConnection {


    private EventLoopGroup group;
    public InetSocketAddress self;

    public TCPOutConnection(InetSocketAddress host) {
        group = createNewWorkerGroup(1);
        this.self = host;
    }

    public void connect(InetSocketAddress peer, TCPMetrics metrics, TCPNettyConsumer consumer, TransmissionType type, String connectionId){
        final Pair identification = Pair.of(peer,connectionId);

        try {
            Bootstrap b = new Bootstrap();

            b.group(group)
                    .channel(socketChannel())
                    .remoteAddress(peer)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                            if(TransmissionType.STRUCTURED_MESSAGE==type){
                                ch.pipeline().addLast(new DelimitedMessageDecoder(metrics,consumer,identification));
                            }else{
                                ch.pipeline().addLast(new StreamMessageDecoder(metrics,consumer,identification));
                            }
                            ch.pipeline().addLast( new TCPOutConHandler(new HandShakeMessage(self,type),consumer,metrics,type,identification));
                    }
                    });

            b.connect().addListener(future -> {
                if(!future.isSuccess()){
                    consumer.handleOpenConnectionFailed(identification,future.cause());
                }
            });
            //printSomeConfigs();
            /***
            updateConfiguration(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,64*1024);
            updateConfiguration(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,2*64*1024);
            updateConfiguration(ChannelOption.AUTO_READ,Boolean.TRUE);**/
        } catch (Exception e) {
            consumer.handleOpenConnectionFailed(identification,e.getCause());
        }
    }

    public static EventLoopGroup createNewWorkerGroup(int nThreads) {
        //if (Epoll.isAvailable()) return new EpollEventLoopGroup(nThreads);
        //else
        return new NioEventLoopGroup(nThreads);
    }
    private Class<? extends Channel> socketChannel(){
        /**
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        }**/
        return NioSocketChannel.class;
    }
    public void shutDown(){
        if(group!=null){
            group.shutdownGracefully();
        }
    }

}
