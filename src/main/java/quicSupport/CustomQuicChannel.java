package quicSupport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.*;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.server.StreamInConnection;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.funcHandlers.QuicFuncHandlers;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.handlers.pipeline.QuicClientChannelConHandler;
import quicSupport.handlers.pipeline.QuicStreamReadHandler;
import quicSupport.handlers.pipeline.ServerChannelInitializer;
import quicSupport.utils.CustomConnection;
import quicSupport.utils.Logics;
import quicSupport.utils.entities.QuicChannelMetrics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public abstract class CustomQuicChannel {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);
    private final QuicListenerExecutor streamEventExecutor;
    @Getter
    private final DefaultEventExecutor executor;
    private final InetSocketAddress self;
    private static boolean enableMetrics;
    public final static String NAME = "QUIC_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8575";

    private final Map<InetSocketAddress, CustomConnection> connections;
    private final Map<String,InetSocketAddress> channelIds; //streamParentID, peer
    private final QuicClientExample client;
    private final Properties properties;
    private QuicChannelMetrics metrics;
    public CustomQuicChannel(Properties properties)throws IOException {
        this.properties=properties;
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        enableMetrics = (boolean) properties.getOrDefault("metrics",true);
        if(enableMetrics){
            metrics=new QuicChannelMetrics(self);
        }
        QuicFuncHandlers streamFuncHandlers = new QuicFuncHandlers(
                this::channelActive,
                this::channelClosed,
                this::onOpenConnectionFailed,
                this::streamCreatedHandler,
                this::streamReader,
                this::streamClosedHandler,
                this::streamErrorHandler);
        streamEventExecutor = new QuicListenerExecutor(StreamInConnection.newDefaultEventExecutor(), streamFuncHandlers);

        connections = new ConcurrentHashMap<>();
        channelIds = new ConcurrentHashMap<>();

        QuicServerExample server = new QuicServerExample(addr.getHostName(), port, streamEventExecutor,metrics,properties);
        client = new QuicClientExample(self,streamEventExecutor,new NioEventLoopGroup(1), metrics);
        executor = streamEventExecutor.getLoop();
        try{
            server.start(this::onServerSocketBind);
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
        //streams.remove(streamId);
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        channelClosed(channel.parent().id().asShortText(),streamId);
        onStreamClosedHandler(peer,channel);
    }
    public abstract void onStreamClosedHandler(InetSocketAddress peer,QuicStreamChannel channel);

    private void streamCreatedHandler(QuicStreamChannel channel) {
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        if(peer!=null){//THE SERVER HAS NOT RECEIVED THE CLIENT'S LISTENING ADDRESS YET
            String streamId = channel.id().asShortText();
            logger.info("{}. STREAM CREATED {}",self,streamId);
            connections.get(peer).addStream(channel);
            onStreamCreatedHandler(peer,channel);
        }
    }
    public abstract void onStreamCreatedHandler(InetSocketAddress peer,QuicStreamChannel channel);

    public void streamReader(String channelId, byte[] bytes){
        logger.info("SELF:{} - STREAM_ID:{}. RECEIVED DATA.",self,channelId);
        onChannelRead(channelId,bytes,channelIds.get(channelId));
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData,InetSocketAddress remotePeer){
        boolean incoming = false;
        try {
            InetSocketAddress listeningAddress;
            HandShakeMessage handShakeMessage=null;
            if(controlData==null){//is OutGoing
                listeningAddress = remotePeer;
            }else{//is InComing
                handShakeMessage = Logics.gson.fromJson(new String(controlData),HandShakeMessage.class);
                listeningAddress =handShakeMessage.getAddress();
                incoming=true;
            }
            connections.put(listeningAddress, new CustomConnection(streamChannel,remotePeer,incoming));
            channelIds.put(streamChannel.parent().id().asShortText(),listeningAddress);

            onChannelActive(streamChannel,handShakeMessage,listeningAddress);
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),incoming);
            }
            logger.info("{} CHANNEL {} TO {} ACTIVATED. INCOMING ? {}",self,streamChannel.parent().id().asShortText(),listeningAddress,incoming);
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }
    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer);



    /**
    private boolean isTheFirstStream(String parentId, String streamId){
        InetSocketAddress peer = channelIds.get(parentId);
        if(peer==null){
            return false;
        }
        QuicStreamChannel streamChannel = connections.get(peer);
        return streamChannel !=null && streamId.equals(streamChannel.id().asShortText());
    } **/
    public  void channelClosed(String channelId, String streamId){
        /**
        if(isTheFirstStream(channelId,streamId)){
            InetSocketAddress peer = channelIds.remove(channelId);
            Channel stream = connections.remove(peer);
            stream.parent().disconnect();
            logger.info("{} CLOSED CONNECTION TO {}",self,peer);
            onChannelClosed(peer);
        }**/
    }
    public abstract void onChannelClosed(InetSocketAddress peer);

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/

    public void openConnection(InetSocketAddress peer) {
        if(connections.containsKey(peer)){
            logger.info("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            logger.info("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer,properties);
            }catch (Exception e){
                streamEventExecutor.onConnectionError(peer,e.getCause());
            }
        }
    }
    public void closeConnection(InetSocketAddress peer){
        CustomConnection connection = connections.remove(peer);
        if(connection==null){
            logger.info("{} IS NOT CONNECTED TO {}",self,peer);
        }else{
            connection.close();
        }
    }
    public QuicConnectionStats getStats(InetSocketAddress peer) throws ExecutionException, InterruptedException, UnknownElement {
        QuicChannel connection = getOrThrow(peer).getConnection();
        return connection.collectStats().get();
    }
    public ConcurrentLinkedQueue<QuicConnectionMetrics> oldMetrics(){
        return metrics.oldConnections;
    }
    public CustomConnection getOrThrow(InetSocketAddress peer) throws UnknownElement {
        CustomConnection quicConnection = connections.get(peer);
        if(quicConnection==null){
            throw new UnknownElement("NO SUCH CONNECTION TO: "+peer);
        }
        return quicConnection;
    }
    public CustomConnection getOrThrow2(String id) throws UnknownElement {
        InetSocketAddress host = channelIds.get(id);
        /**
        CustomConnection strcueamChannel = connections.get(host);
        if(streamChannel==null){
            throw new UnknownElement("UNKNOWN STREAM ID: "+id);
        }***/
        return getOrThrow(host);
    }
    public void createStream(InetSocketAddress peer) throws Exception {
         QuicChannel quicChannel = getOrThrow(peer).getConnection();
         Logics.createStream(quicChannel,streamEventExecutor,metrics);
    }
    public void closeStream(String streamId) throws UnknownElement {
        /**
        QuicStreamChannel quicStreamChannel = getOrThrow2(streamId);
        quicStreamChannel.shutdown();
        quicStreamChannel.disconnect();
         **/
    }
    public void send(String streamId, byte [] message, int len) throws UnknownElement {
        send(streamId,message,len, null);
    }
    public void send(String streamId,byte[] message, int len, Promise<Void> promise) throws UnknownElement {
        send(getOrThrow2(streamId),message,len,promise);
    }
    public void send(InetSocketAddress peer,byte[] message, int len, Promise<Void> promise) throws UnknownElement {
        send(getOrThrow(peer),message,len,promise);
    }
    private void send(CustomConnection connection, byte[] message, int len, Promise<Void> promise) throws UnknownElement {
        try{
            if(connection.connectionDown()){
                logger.info("CONNECTION {} IS DOWN. INCOMING ? {}",connection.getRemote(),connection.isInComing());
                return;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        ChannelFuture f = connection.getDefaultStream().writeAndFlush(Logics.writeBytes(len,message, QuicStreamReadHandler.APP_DATA));
        if(promise!=null){
            f.addListener(new PromiseNotifier<>(promise));
        }
    }
    public abstract void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

    /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/


    protected void onOutboundConnectionUp() {}


    protected void onOutboundConnectionDown() {}

    public void end(){
        /**
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
         **/
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
