package org.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;

public class StreamSender {
    private final String host;
    private final int port;

    private Channel channel;
    private EventLoopGroup group;
    private ChannelHandlerContext channelHandlerContext;
    public StreamSender(String host, int port) {
        this.host = host;
        this.port = port;
        channelHandlerContext=null;
    }

    public void connect() throws Exception {
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    //.option(ChannelOption.SO_SNDBUF, 64 * 1024)
                    //.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    //.option(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(true, ByteOrder.BIG_ENDIAN))
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast( new StreamSenderHandler());
                    }
                    });
            channel = b.connect().sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void printSomeConfigs(){
        System.out.println("SO_SNDBUF: "+channel.config().getOptions().get(ChannelOption.SO_SNDBUF));
        System.out.println("WRITE_BUFFER_LOW_WATER_MARK "+channel.config().getOptions().get(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK));
        System.out.println("WRITE_BUFFER_HIGH_WATER_MARK "+channel.config().getOptions().get(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK));
        System.out.println("Automatic flash: "+channel.config().isAutoRead());
        System.out.println("TIMES SENT "+timesSent);
        System.out.println("CHANNEL ID: "+channel.id().asLongText());
    }
    public void close(){
        channel.close();
        group.shutdownGracefully();
    }
    public void flushMessages(){
        System.out.println("FLUSHED!");
        //channel.flush();
    }
    public void sendMessage(byte [] message, int len){
        timesSent += message.length;
        channel.writeAndFlush(Unpooled.copiedBuffer(message,0,len)).addListener(future -> {
            if(future.isSuccess()){
                System.out.println("MESSAGE SENT!");
            }else {
                System.out.println("MESSAGE NOT SENT: "+future.cause());
            }
        });
    }

    public boolean canSend(){
        return channel.isWritable();
    }
    int timesSent = 0;
    public void sendMessage(String message){
        channel.write(Unpooled.copiedBuffer(message,
                CharsetUtil.UTF_8)).addListener(future -> {
            if(future.isSuccess()){
                System.out.println("MESSAGE SENT!");
            }else {
                System.out.println("MESSAGE NOT SENT: "+future.cause());
            }
        });
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println(
                    "Usage: " + StreamSender.class.getSimpleName() +
                            " <host> <port>");
            return;
        }
        /**
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        StreamSender streamSender = new StreamSender(host, port);
        streamSender.connect();
        streamSender.keepRunning();**/
    }

    public void keepRunning() throws IOException {
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            if("quit".equalsIgnoreCase(userInput)){
                System.out.println("CLOSING!");
                break;
            }else if("flush".equalsIgnoreCase(userInput)){
                flushMessages();
            }
            /**else{
                System.out.println("Sent: " + userInput);
                sendMessage(userInput);
            }**/
        }
    }

}
