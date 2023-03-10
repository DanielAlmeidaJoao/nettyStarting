package org.streamingAPI.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import lombok.Getter;
import org.streamingAPI.handlerFunctions.receiver.*;
import org.streamingAPI.server.channelHandlers.CustomHandshakeHandler;
import org.streamingAPI.server.channelHandlers.encodings.DelimitedMessageDecoder;
import org.streamingAPI.server.channelHandlers.StreamReceiverHandler;
import org.streamingAPI.server.channelHandlers.encodings.StreamMessageDecoder;
import org.streamingAPI.server.listeners.InNettyChannelListener;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class StreamInConnection {

    public static String NAME = "STREAM_RECEIVER";

    //One of the main advantages of using a single thread to
    // execute tasks is that it eliminates the need for
    // synchronization primitives such as locks and semaphores.


    private final int port;
    private final String hostName;
    private Channel serverChannel;

    @Getter
    private InNettyChannelListener inListener;
    private Map<String,SocketChannel> clients;

    public StreamInConnection(String hostName, int port,
                              ChannelFuncHandlers handlerFunctions) {
        this(hostName,port,new InNettyChannelListener(newDefaultEventExecutor(),handlerFunctions));
    }
    public StreamInConnection(String hostName, int port, InNettyChannelListener listener) {
        this.port = port;
        this.hostName = hostName;
        this.inListener = listener;
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
    public void startListening(boolean sync, boolean readDelimited) throws Exception{
        EventLoopGroup parentGroup = createNewWorkerGroup();
        EventLoopGroup childGroup = createNewWorkerGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(parentGroup,childGroup)
                .channel(socketChannel())
                .localAddress(new InetSocketAddress(hostName,port))
                .childHandler(new ChannelInitializer<SocketChannel>(){
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(CustomHandshakeHandler.NAME,new CustomHandshakeHandler(inListener));
                        if(readDelimited){
                            ch.pipeline().addLast(new DelimitedMessageDecoder());
                        }else{
                            ch.pipeline().addLast(new StreamMessageDecoder());
                        }
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
        // Wait for the server channel to close. Blocks.
        f.addListener(future -> {
            //TO DO
            System.out.println("CONNECTION CLOSED "+future.isSuccess());
            parentGroup.shutdownGracefully().sync();
            childGroup.shutdownGracefully().sync();
        });
    }
    private void noSuchStreamException(String streamId){
        if(!clients.containsKey(streamId)){
            throw new NoSuchElementException("NO SUCH STREAM ID: "+streamId);
        }
    }
    public void send(String streamId , byte[] message, int len){
        sendBytesWithListener(streamId,message,len,null);
    }
    public DefaultEventExecutor getDefaultEventExecutor(){
        return inListener.getLoop();
    }
    /**
     * DefaultEventExecutor loop = ...
     * Promise<Void> promise = loop.newPromise();
    *         promise.addListener(future -> {
    *             if (future.isSuccess() && triggerSent) listener.messageSent(msg, peer);
    *             else if (!future.isSuccess()) listener.messageFailed(msg, peer, future.cause());
    *         });
     * @param streamId
     * @param message
     * @param len
     * @param promise
     */
    public void sendBytesWithListener(String streamId, byte[] message, int len, Promise<Void> promise){
        SocketChannel stream = clients.get(streamId);
        noSuchStreamException(streamId);
        ChannelFuture f = stream.writeAndFlush(Unpooled.copiedBuffer(message,0,len));
        if(promise!=null){
            f.addListener(new PromiseNotifier<>(promise));
        }
    }
    public <T> void updateConfiguration(ChannelOption<T> option, T value) {
        serverChannel.config().setOption(option,value);
    }

    public <T> void updateConfiguration(String streamId,ChannelOption<T> option, T value) {
        noSuchStreamException(streamId);
        clients.get(streamId).config().setOption(option,value);
    }


    public void close(){
        serverChannel.disconnect();
    }
    private void printSomeConfigs(String streamid){
        System.out.println("CONFIGS:");
        SocketChannel channel = clients.get(streamid);
        System.out.println(channel.config().getOptions().get(ChannelOption.SO_RCVBUF));
        System.out.println(channel.config().getMaxMessagesPerRead());
    }
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
