package tcpSupport.tcpChannelAPI.connectionSetups;

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tcpSupport.tcpChannelAPI.channel.StreamingNettyConsumer;
import tcpSupport.tcpChannelAPI.pipeline.TCPCustomHandshakeHandler;

import java.net.InetSocketAddress;
import java.util.Properties;

public class TCPServerEntity implements ServerInterface{
    //One of the main advantages of using a single thread to
    // execute tasks is that it eliminates the need for
    // synchronization primitives such as locks and semaphores.



    private static final Logger logger = LogManager.getLogger(TCPServerEntity.class);

    private final int port;
    private final String hostName;
    private Channel serverChannel;
    private final StreamingNettyConsumer consumer;
    private Properties properties;
    private EventLoopGroup parentGroup;
    public TCPServerEntity(String hostName, int port, StreamingNettyConsumer consumer, Properties properties) {
        this.port = port;
        this.hostName = hostName;
        this.consumer = consumer;
        this.properties = properties;
        int serverThreads = FactoryMethods.serverThreads(properties);
        this.parentGroup = createNewWorkerGroup(serverThreads);
    }

    public void startServer()
            throws Exception{
        //EventLoopGroup childGroup = createNewWorkerGroup(serverThreads);
        ServerBootstrap b = new ServerBootstrap();
        b.group(parentGroup).channel(socketChannel())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .localAddress(new InetSocketAddress(hostName,port))
                .childHandler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        //TALK ABOUT TCP FAST OPEN IN THESIS
                        ch.pipeline().addLast(TCPCustomHandshakeHandler.NAME,new TCPCustomHandshakeHandler(consumer));
                        //ch.pipeline().addLast(new TCPServerNettyHandler(metrics,consumer));
                    }
                });
        ChannelFuture f = b.bind().sync().addListener(future ->
                consumer.onServerSocketBind(future.isSuccess(),future.cause())
        );
        serverChannel = f.channel();
        serverChannel.closeFuture().addListener(future -> {
            parentGroup.shutdownGracefully().getNow();
            //childGroup.shutdownGracefully().getNow();
            logger.debug("Server socket closed. " + (future.isSuccess() ? "" : "Cause: " + future.cause()));
        });
    }
    public void shutDown(){
        serverChannel.close();
        serverChannel.disconnect();
    }

    @Override
    public EventLoopGroup getEventLoopGroup() {
        return parentGroup;
    }

    public <T> void updateConfiguration(ChannelOption<T> option, T value) {
        serverChannel.config().setOption(option,value);
    }

    public static EventLoopGroup createNewWorkerGroup(int numberOfThreads) {
        //if (Epoll.isAvailable()) return new EpollEventLoopGroup(nThreads);
        //else
        if(Epoll.isAvailable()){
            return new EpollEventLoopGroup(numberOfThreads);
        }else{
            return new NioEventLoopGroup(numberOfThreads);
        }
    }
    private Class<? extends ServerChannel> socketChannel(){
        if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        }else{
            return NioServerSocketChannel.class;
        }
    }

}
