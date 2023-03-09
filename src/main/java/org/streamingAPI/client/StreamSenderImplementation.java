package org.streamingAPI.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import lombok.Setter;
import org.streamingAPI.client.channelHandlers.StreamSenderHandler;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.listeners.InChannelListener;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.streamingAPI.server.StreamReceiverImplementation.newDefaultEventExecutor;

public class StreamSenderImplementation implements StreamSender {
    @Setter
    private String host;
    @Setter
    private int port;
    private final InChannelListener inChannelListener;

    private Channel channel;
    private EventLoopGroup group;
    public StreamSenderImplementation(String host, int port, ChannelFuncHandlers handlerFunctions) {
        this.host = host;
        this.port = port;
        this.inChannelListener = new org.streamingAPI.server.listeners.InChannelListener(newDefaultEventExecutor(),handlerFunctions);

    }

    @Override
    public void connect(){
        group = createNewWorkerGroup(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(socketChannel())
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast( new StreamSenderHandler("THE NEW GUY IN TONW ".getBytes(StandardCharsets.UTF_8),inChannelListener,false));
                    }
                    });
            channel = b.connect().sync().channel();

            //printSomeConfigs();
            /***
            updateConfiguration(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,64*1024);
            updateConfiguration(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,2*64*1024);
            updateConfiguration(ChannelOption.AUTO_READ,Boolean.TRUE);**/
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printSomeConfigs(){
        System.out.println("CONFIGS:");
        System.out.println(channel.config().getOptions().get(ChannelOption.SO_RCVBUF));
        System.out.println(channel.config().getMaxMessagesPerRead());
    }

    /**
     * @pre Connection must be established/active before calling this method
     * @param option Netty option
     * @param value the new value
     * @param <T> primitive type of the value
     */
    @Override
    public <T> void updateConfiguration(ChannelOption<T> option, T value){
        channel.config().setOption(option,value);
    }

    /**
     * Closes the connection after sending all pending data
     */
    @Override
    public void close(){
        try {
            while (channel.unsafe().outboundBuffer().totalPendingWriteBytes()>0){
                Thread.sleep(1000);
            }
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
    @Override
    public void send(byte[] message, int len){
        sendWithListener(message,len, null);
    }
    public void sendWithListener(byte[] message, int len, Promise<Void> promise){
        sendDelimited(Unpooled.copiedBuffer(message,0,len), promise);
    }
    public void sendDelimited(ByteBuf byteBuf, Promise<Void> promise){
        ChannelFuture f = channel.writeAndFlush(byteBuf);
        if (promise!=null){
            f.addListener(new PromiseNotifier<>(promise));
        }
    }
    @Override
    public String streamId(){
        return channel.id().asShortText();
    }

    @Override
    public void setHost(String hostname, int port) {
        setHost(hostname);
        setPort(port);
    }

    public static EventLoopGroup createNewWorkerGroup(int nThreads) {
        //if (Epoll.isAvailable()) return new EpollEventLoopGroup(nThreads);
        //else
        return new NioEventLoopGroup();
    }
    private Class<? extends Channel> socketChannel(){
        /**
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        }**/
        return NioSocketChannel.class;
    }

    public DefaultEventExecutor getDefaultEventExecutor(){
        return inChannelListener.getLoop();
    }
}
