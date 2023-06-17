package org.tcpStreamingAPI.connectionSetups;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.channel.StreamingNettyConsumer;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.pipeline.CustomHandshakeHandler;

import java.net.InetSocketAddress;

public class StreamInConnection {
    //One of the main advantages of using a single thread to
    // execute tasks is that it eliminates the need for
    // synchronization primitives such as locks and semaphores.



    private static final Logger logger = LogManager.getLogger(StreamInConnection.class);

    private final int port;
    private final String hostName;
    private Channel serverChannel;
    public StreamInConnection(String hostName, int port) {
        this.port = port;
        this.hostName = hostName;

    }


    public static DefaultEventExecutor newDefaultEventExecutor(){
        return new DefaultEventExecutor();
    }
    /**
     *
     * @param sync whether to block the main thread or not
     * @throws Exception
     */
    public void startListening(boolean sync,TCPStreamMetrics metrics, StreamingNettyConsumer consumer)
            throws Exception{
        EventLoopGroup parentGroup = createNewWorkerGroup();
        EventLoopGroup childGroup = createNewWorkerGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(parentGroup,childGroup).channel(socketChannel())
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .localAddress(new InetSocketAddress(hostName,port))
                .childHandler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        System.out.println("MUST BE CALLED FOR EACH INCOMMING CONNECTION");
                        String connectionId = consumer.nextId();
                        ch.pipeline().addLast(CustomHandshakeHandler.NAME,new CustomHandshakeHandler(metrics,consumer,connectionId));
                        //ch.pipeline().addLast(DelimitedMessageDecoder.NAME,new DelimitedMessageDecoder(metrics,consumer,connectionId));
                        //ch.pipeline().addLast(new StreamReceiverHandler(metrics,consumer,connectionId));
                    }
                });
        ChannelFuture f = b.bind().sync().addListener(future ->
                consumer.onServerSocketBind(future.isSuccess(),future.cause())
        );
        serverChannel = f.channel();
        if(sync){
            f = serverChannel.closeFuture().sync();
        }else{
            f = serverChannel.closeFuture();
        }

        // Wait for the server channel to close. Blocks.
        f.addListener(future -> {
            parentGroup.shutdownGracefully().getNow();
            childGroup.shutdownGracefully().getNow();
            logger.debug("Server socket closed. " + (future.isSuccess() ? "" : "Cause: " + future.cause()));
        });
    }
    public void closeServerSocket(){
        serverChannel.close();
        serverChannel.disconnect();
    }

    public <T> void updateConfiguration(ChannelOption<T> option, T value) {
        serverChannel.config().setOption(option,value);
    }

    public static EventLoopGroup createNewWorkerGroup() {
        //if (Epoll.isAvailable()) return new EpollEventLoopGroup(nThreads);
        //else
        return new NioEventLoopGroup();
    }
    private Class<? extends ServerChannel> socketChannel(){
        /**if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        }**/
        return NioServerSocketChannel.class;
    }

}
