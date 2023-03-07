package org.streamingAPI.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class StreamSenderImplementation implements StreamSender {
    private final String host;
    private final int port;

    private Channel channel;
    private EventLoopGroup group;
    public StreamSenderImplementation(String host, int port) {
        this.host = host;
        this.port = port;
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
                        ch.pipeline().addLast( new StreamSenderHandler("THE NEW GUY IN TONW ".getBytes(StandardCharsets.UTF_8)));
                    }
                    });
            channel = b.connect().sync().channel();
            /**
            printSomeConfigs();
            updateConfiguration(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK,64*1024);
            updateConfiguration(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK,2*64*1024);
            updateConfiguration(ChannelOption.AUTO_READ,Boolean.TRUE);
             **/
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    public void sendBytes(byte[] message, int len){
        channel.writeAndFlush(Unpooled.copiedBuffer(message,0,len)).addListener(future -> {
            if(future.isSuccess()){
                //TODO metrics!
            }else {
                System.out.println("MESSAGE NOT SENT: "+future.cause());
            }
        });
    }
    @Override
    public String streamId(){
        return channel.id().asShortText();
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
}
