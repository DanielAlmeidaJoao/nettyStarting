package org.streamingAPI.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.streamingAPI.handlerFunctions.receiver.*;
import org.streamingAPI.server.channelHandlers.CustomHandshakeHandler;
import org.streamingAPI.server.channelHandlers.StreamReceiverHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;

public class StreamReceiverImplementation implements StreamReceiver {

    public static String NAME = "STREAM_RECEIVER";

    //One of the main advantages of using a single thread to
    // execute tasks is that it eliminates the need for
    // synchronization primitives such as locks and semaphores.


    private final int port;

    private final String hostName;
    private Channel serverChannel;

    private org.streamingAPI.server.listeners.InChannelListener inListener;
    private Map<String,SocketChannel> clients;

    public StreamReceiverImplementation(String hostName, int port,
                                        ChannelHandlers handlerFunctions
                                        ) {
        this.port = port;
        this.hostName = hostName;
        this.inListener = new org.streamingAPI.server.listeners.InChannelListener(newDefaultEventExecutor(),handlerFunctions);
        clients = new HashMap<>();
    }

    public static DefaultEventExecutor newDefaultEventExecutor(){
        return new DefaultEventExecutor();
    }

    /**
     *
     * @param sync whether to block the main thread or not
     * @throws Exception
     */
    @Override
    public void startListening(boolean sync) throws Exception {
        EventLoopGroup parentGroup = createNewWorkerGroup();
        EventLoopGroup childGroup = createNewWorkerGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup,childGroup)
                    .channel(socketChannel())
                    .localAddress(new InetSocketAddress(hostName,port))
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(CustomHandshakeHandler.NAME,new CustomHandshakeHandler(inListener));
                            ch.pipeline().addLast(new StreamReceiverHandler(inListener));
                            clients.put(ch.id().asShortText(),ch);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            serverChannel = f.channel();

            if(sync){
                f = serverChannel.closeFuture().sync();
            }else{
                f = serverChannel.closeFuture();
            }
            System.out.println("GOING TO SLEEP!1");
            // Wait for the server channel to close. Blocks.
            f.addListener(future -> {
                //TO DO
                System.out.println("CONNECTION CLOSED "+future.isSuccess());
                parentGroup.shutdownGracefully().sync();
                childGroup.shutdownGracefully().sync();
            });
            System.out.println("GOING TO SLEEP!");
            Thread.sleep(100*1000);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void noSuchStreamException(String streamId){
        if(!clients.containsKey(streamId)){
            throw new NoSuchElementException("NO SUCH STREAM ID: "+streamId);
        }
    }
    public void sendBytes(String streamId ,byte[] message, int len){
        SocketChannel stream = clients.get(streamId);
        noSuchStreamException(streamId);
        stream.writeAndFlush(Unpooled.copiedBuffer(message,0,len)).addListener(future -> {
            if(future.isSuccess()){
                //TODO metrics!
            }else {
                System.out.println("MESSAGE NOT SENT: "+future.cause());
            }
        });
    }
    @Override
    public <T> void updateConfiguration(ChannelOption<T> option, T value) {
        serverChannel.config().setOption(option,value);
    }

    @Override
    public <T> void updateConfiguration(String streamId,ChannelOption<T> option, T value) {
        noSuchStreamException(streamId);
        clients.get(streamId).config().setOption(option,value);
    }


    @Override
    public void close(){
        serverChannel.disconnect();
    }
    private void printSomeConfigs(String streamid){
        System.out.println("CONFIGS:");
        SocketChannel channel = clients.get(streamid);
        System.out.println(channel.config().getOptions().get(ChannelOption.SO_RCVBUF));
        System.out.println(channel.config().getMaxMessagesPerRead());
    }
    @Override
    public void closeStream(String streamId){
        noSuchStreamException(streamId);
        printSomeConfigs(streamId);
        clients.remove(streamId).disconnect();
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
