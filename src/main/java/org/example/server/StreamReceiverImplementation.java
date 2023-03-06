package org.example.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.example.handlerFunctions.receiver.StreamReceiverChannelActiveFunction;
import org.example.handlerFunctions.receiver.StreamReceiverEOSFunction;
import org.example.handlerFunctions.receiver.StreamReceiverFunction;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class StreamReceiverImplementation implements StreamReceiver {
    private final int port;

    private final String hostName;
    private Channel serverChannel;

    private StreamReceiverChannelActiveFunction activeFunction;
    private StreamReceiverFunction streamReceiverFunction;
    private StreamReceiverEOSFunction streamReceiverEOSFunction;
    private Map<String,SocketChannel> clients;

    public StreamReceiverImplementation(String hostName, int port,
                                        StreamReceiverChannelActiveFunction chanActiveFunc,
                                        StreamReceiverFunction receiveDataHandler,
                                        StreamReceiverEOSFunction chanInactiveHandler
                                        ) {
        this.port = port;
        this.hostName = hostName;
        activeFunction = chanActiveFunc;
        this.streamReceiverFunction = receiveDataHandler;
        this.streamReceiverEOSFunction = chanInactiveHandler;
        clients = new HashMap<>();
    }

    @Override
    public void startListening() throws Exception {
        EventLoopGroup parentGroup = createNewWorkerGroup(1);
        EventLoopGroup childGroup = createNewWorkerGroup(1);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup,childGroup)
                    .channel(socketChannel())
                    .localAddress(new InetSocketAddress(hostName,port))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            //ch.pipeline().addLast(CustomHandshakeHandler.NAME,new CustomHandshakeHandler(test));
                            ch.pipeline().addLast(new StreamReceiverHandler(activeFunction,
                                    streamReceiverFunction,
                                    streamReceiverEOSFunction));
                            clients.put(ch.id().asShortText(),ch);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            serverChannel = f.channel();

            // Wait for the server channel to close. Blocks.
            serverChannel.closeFuture().sync().addListener(future -> {
                //TO DO
                System.out.println("CONNECTION CLOSED "+future.isSuccess());
                parentGroup.shutdownGracefully().sync();
                childGroup.shutdownGracefully().sync();
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public <T> void updateConfiguration(ChannelOption<T> option, T value) {
        serverChannel.config().setOption(option,value);
    }


    @Override
    public void close(){
        serverChannel.disconnect();
    }
    @Override
    public void closeStream(String streamId){
        clients.remove(streamId).disconnect();
    }

    public static EventLoopGroup createNewWorkerGroup(int nThreads) {
        //if (Epoll.isAvailable()) return new EpollEventLoopGroup(nThreads);
        //else
        return new NioEventLoopGroup(nThreads);
    }
    private Class<? extends ServerChannel> socketChannel(){
        /**if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        }**/
        return NioServerSocketChannel.class;
    }

}
