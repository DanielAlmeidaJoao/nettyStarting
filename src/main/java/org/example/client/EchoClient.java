package org.example.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

public class EchoClient {
    private final String host;
    private final int port;

    private Channel channel;
    private EventLoopGroup group;
    private ChannelHandlerContext channelHandlerContext;
    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
        channelHandlerContext=null;
    }

    public void start() throws Exception {
        group = new NioEventLoopGroup();
        EchoClientHandler echoClientHandler = new EchoClientHandler();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(echoClientHandler);
                    }
                    });
            channel = b.connect().sync().channel();
            System.out.println(echoClientHandler.getCtx());
            channelHandlerContext = echoClientHandler.getCtx();
            System.out.println(channelHandlerContext);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void closeConnection(){
        System.out.println("CHANNEL CLOSED!");
        try {
            channel.closeFuture().sync();
        }catch (Exception e){

        }finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void sendMessage(String message){
        if(channelHandlerContext==null){
            return;
        }
        channelHandlerContext.writeAndFlush(Unpooled.copiedBuffer(message,
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
                    "Usage: " + EchoClient.class.getSimpleName() +
                            " <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        EchoClient echoClient = new EchoClient(host, port);
        echoClient.start();

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            if("quit".equalsIgnoreCase(userInput)){
                System.out.println("CLOSING!");
                echoClient.closeConnection();
            }else{
                System.out.println("Sent: " + userInput);
                echoClient.sendMessage(userInput);
            }
        }
    }

}
