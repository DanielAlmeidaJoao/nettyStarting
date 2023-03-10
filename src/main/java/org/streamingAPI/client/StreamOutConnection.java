package org.streamingAPI.client;

import com.google.gson.Gson;
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
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.server.listeners.InNettyChannelListener;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.streamingAPI.server.StreamInConnection.newDefaultEventExecutor;

public class StreamOutConnection {

    public static Gson g = new Gson();

    private HandShakeMessage handShakeMessage;
    private byte [] handshake;
    @Setter
    private String host;
    @Setter
    private int port;
    private final InNettyChannelListener inNettyChannelListener;

    private Channel channel;
    private EventLoopGroup group;

    public StreamOutConnection(ChannelFuncHandlers handlerFunctions) {
        this(new InNettyChannelListener(newDefaultEventExecutor(),handlerFunctions));
    }
    public StreamOutConnection(InNettyChannelListener listener) {
        this.inNettyChannelListener = listener;
        group = createNewWorkerGroup(1);
    }

    public void connect(String host, int port){
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(socketChannel())
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast( new StreamSenderHandler(handshake,inNettyChannelListener,false));
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
    public <T> void updateConfiguration(ChannelOption<T> option, T value){
        channel.config().setOption(option,value);
    }

    /**
     * Closes the connection after sending all pending data
     */
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
    public String streamId(){
        return channel.id().asShortText();
    }

    public void setHost(String hostname, int port) {
        setHost(hostname);
        setPort(port);
        handShakeMessage = new HandShakeMessage(hostname,port);
        handshake = g.toJson(handShakeMessage).getBytes();
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
        return inNettyChannelListener.getLoop();
    }
}
