package org.example.p2p;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.util.Scanner;

public class PeerToPeerChannel {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // Start the first peer
        int port1 = 8000;
        Channel channel1 = startPeer(port1);

        // Start the second peer
        int port2 = 8001;
        Channel channel2 = startPeer(port2);

        System.out.println("Enter message (type 'quit' to exit):");

        while (true) {
            String message = scanner.nextLine();

            if (message.equals("quit")) {
                break;
            }

            // Send the message from the first peer to the second peer
            ByteBuf buffer = channel1.alloc().buffer();
            buffer.writeBytes(message.getBytes());
            channel2.writeAndFlush(buffer);
        }

        // Close the channels and exit
        channel1.close().sync();
        channel2.close().sync();
    }

    private static Channel startPeer(int port) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                 .channel(NioSocketChannel.class)
                 .handler(new LoggingHandler(LogLevel.INFO))
                 .handler(new ChannelInitializer<SocketChannel>() {
                     @Override
                     protected void initChannel(SocketChannel ch) throws Exception {
                         ch.pipeline().addLast(new ChannelHandlerAdapter() {
                             @Override
                             public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                 // Handle incoming message
                                 System.out.println("Received message: " + msg.toString());
                             }
                         });
                     }
                 });

        return bootstrap.connect("localhost", port).sync().channel();
    }
}
