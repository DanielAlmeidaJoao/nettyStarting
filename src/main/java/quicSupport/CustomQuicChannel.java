package quicSupport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicConnectionStats;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.StreamInConnection;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.exceptions.UnknownElement;
import quicSupport.handlers.client.QuicStreamReadHandler;
import quicSupport.handlers.funcHandlers.QuicFuncHandlers;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public abstract class CustomQuicChannel {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);
    private final QuicListenerExecutor streamEventExecutor;
    @Getter
    private DefaultEventExecutor executor;
    private InetSocketAddress self;
    public final static String NAME = "QUIC_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8575";

    private Map<InetSocketAddress,QuicStreamChannel> connections;
    private Map<String,InetSocketAddress> channelIds; //streamParentID, peer

    private Map<String,QuicStreamChannel> streams;
    private QuicServerExample server;
    private QuicClientExample client;

    public CustomQuicChannel(Properties properties)throws IOException {
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);

        QuicFuncHandlers streamFuncHandlers = new QuicFuncHandlers(
                this::channelActive,
                this::channelClosed,
                this::onOpenConnectionFailed,
                this::streamCreatedHandler,
                this::channelRead,
                this::streamClosedHandler,
                this::streamErrorHandler);
        streamEventExecutor = new QuicListenerExecutor(StreamInConnection.newDefaultEventExecutor(), streamFuncHandlers);

        connections = new ConcurrentHashMap<>();
        streams = new ConcurrentHashMap<>();
        channelIds = new ConcurrentHashMap<>();

        server = new QuicServerExample(addr.getHostName(),port,streamEventExecutor);
        client = new QuicClientExample(self,streamEventExecutor,new NioEventLoopGroup(1));
        executor = streamEventExecutor.getLoop();
        try{
            server.start();
        }catch (Exception e){
            throw new IOException(e);
        }
    }

    /*********************************** Stream Handlers **********************************/

    private void streamErrorHandler(QuicStreamChannel channel, Throwable throwable) {
        logger.info("{} STREAM {} ERROR: {}",self,channel.id().asShortText(),throwable);
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        onStreamErrorHandler(peer,channel);
    }
    public abstract void onStreamErrorHandler(InetSocketAddress peer,QuicStreamChannel channel);

    private void streamClosedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM {} CLOSED",self,streamId);
        streams.remove(streamId);
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        channelClosed(channel.parent().id().asShortText(),streamId);
        onStreamClosedHandler(peer,channel);
    }
    public abstract void onStreamClosedHandler(InetSocketAddress peer,QuicStreamChannel channel);

    private void streamCreatedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM CREATED {}",self,streamId);
        streams.put(streamId,channel);
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        onStreamCreatedHandler(peer,channel);
    }
    public abstract void onStreamCreatedHandler(InetSocketAddress peer,QuicStreamChannel channel);

    public void channelRead(String channelId, byte[] bytes){
        logger.info("SELF:{} - STREAM_ID:{}. RECEIVED DATA.",self,channelId);
        onChannelRead(channelId,bytes,channelIds.get(channelId));
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    /*********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel channel, HandShakeMessage handShakeMessage, boolean incoming){
        InetAddress hostName;
        int port;
        try {
            hostName = InetAddress.getByName( handShakeMessage.getHostName());
            port = handShakeMessage.getPort();
            InetSocketAddress listeningAddress = new InetSocketAddress(hostName,port);
            logger.info("{} CHANNEL TO {} ACTIVATED. INCOMING ? {}",self,listeningAddress,incoming);
            connections.put(listeningAddress,channel);
            channelIds.put(channel.parent().id().asShortText(),listeningAddress);
            onChannelActive(channel,handShakeMessage,listeningAddress);
            //logger.info("CONNECTION TO {} ACTIVATED.",listeningAddress);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }
    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer);



    private boolean isTheFirstStream(String parentId, String streamId){
        InetSocketAddress peer = channelIds.get(parentId);
        if(peer==null){
            return false;
        }
        QuicStreamChannel streamChannel = connections.get(peer);
        return streamChannel !=null && streamId.equals(streamChannel.id().asShortText());
    }
    public  void channelClosed(String channelId, String streamId){
        System.out.println("FIRST CALLEDDDD");
        if(isTheFirstStream(channelId,streamId)){
            InetSocketAddress peer = channelIds.remove(channelId);
            Channel stream = connections.remove(peer);
            //stream.parent().disconnect();
            logger.info("{} CLOSED CONNECTION TO {}",self,peer);
            onChannelClosed(peer);
        }
    }
    public abstract void onChannelClosed(InetSocketAddress peer);



    public abstract void channelReadConfigData(String s, byte[] bytes);

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/


    public void openConnection(InetSocketAddress peer) {
        if(connections.containsKey(peer)){
            logger.info("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            logger.info("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public void closeConnection(InetSocketAddress peer){
        QuicStreamChannel streamConnection = connections.get(peer);
        if(streamConnection==null){
            logger.info("{} IS NOT CONNECTED TO {}",self,peer);
        }else{
            //streamConnection.parent().close();
            streamConnection.shutdown();
            streamConnection.disconnect();
        }
    }
    public QuicConnectionStats getStats(InetSocketAddress peer) throws ExecutionException, InterruptedException, UnknownElement {
        QuicStreamChannel connection = getOrThrow(peer);
        return connection.parent().collectStats().get();
    }
    public QuicStreamChannel getOrThrow(InetSocketAddress peer) throws UnknownElement {
        QuicStreamChannel quicChannel = connections.get(peer);
        if(quicChannel==null){
            throw new UnknownElement("NO SUCH CONNECTION TO: "+peer);
        }
        return quicChannel;
    }
    public QuicStreamChannel getOrThrow2(String id) throws UnknownElement {
        QuicStreamChannel streamChannel = streams.get(id);
        if(streamChannel==null){
            throw new UnknownElement("UNKNOWN STREAM ID: "+id);
        }
        return streamChannel;
    }
    public void createStream(InetSocketAddress peer) throws Exception {
         QuicChannel quicChannel = getOrThrow(peer).parent();
         QuicClientExample.createStream(quicChannel,new QuicStreamReadHandler(streamEventExecutor),true);
    }
    public void closeStream(String streamId) throws UnknownElement {
        QuicStreamChannel quicStreamChannel = getOrThrow2(streamId);
        quicStreamChannel.shutdown();
        quicStreamChannel.disconnect();
    }
    public void send(String streamId, byte [] message, int len) throws UnknownElement {
        send(streamId,message,len, null);
    }
    public void send(String streamId,byte[] message, int len, Promise<Void> promise) throws UnknownElement {
        QuicStreamChannel quicStreamChannel = getOrThrow2(streamId);
        send(quicStreamChannel,message,len,promise);
    }
    public void send(InetSocketAddress peer,byte[] message, int len, Promise<Void> promise) throws UnknownElement {
        QuicStreamChannel quicStreamChannel = getOrThrow(peer);
        send(quicStreamChannel,message,len,promise);
    }
    private void send(QuicStreamChannel quicStreamChannel,byte[] message, int len, Promise<Void> promise) throws UnknownElement {
        ByteBuf buf = Unpooled.buffer(len+4);
        buf.writeInt(len);
        buf.writeBytes(message);
        ChannelFuture f = quicStreamChannel.writeAndFlush(buf);
        if(promise!=null){
            f.addListener(new PromiseNotifier<>(promise));
        }
    }
    public abstract void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/


    protected void onOutboundConnectionUp() {}


    protected void onOutboundConnectionDown() {
    }

    public void end(){
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("SHUTTING DOWN");
            ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            connections.forEach((inetSocketAddress, channel) -> {
                channelGroup.add(channel);
            });
            try {
                channelGroup.close().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    protected void onInboundConnectionUp() {

    }

    protected void onInboundConnectionDown() {

    }

    public void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }

    public void onServerSocketClose(boolean success, Throwable cause) {
        logger.debug("Server socket closed. " + (success ? "" : "Cause: " + cause));
    }

}
