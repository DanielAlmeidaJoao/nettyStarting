package quicSupport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
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
import quicSupport.handlers.client.QuicStreamReadHandler;
import quicSupport.handlers.funcHandlers.StreamFuncHandlers;
import quicSupport.handlers.funcHandlers.StreamListenerExecutor;
import quicSupport.utils.Logic;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

public abstract class CustomQuicChannel {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);
    private final InNettyChannelListener channelEventExecutor;
    private final StreamListenerExecutor streamEventExecutor;
    @Getter
    private DefaultEventExecutor executor;
    private InetSocketAddress self;
    public final static String NAME = "QUIC_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8575";

    private Map<InetSocketAddress,QuicChannel> connections;
    private Map<String,InetSocketAddress> channelIds;

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
        ChannelFuncHandlers handlers = new ChannelFuncHandlers(this::channelActive,
                this::channelReadConfigData,
                this::channelRead,
                this::channelClosed,
                this::onOpenConnectionFailed);
        channelEventExecutor = new InNettyChannelListener(StreamInConnection.newDefaultEventExecutor(), handlers);

        StreamFuncHandlers streamFuncHandlers = new StreamFuncHandlers(
                this::streamCreatedHandler,
                this::streamClosedHandler,
                this::streamErrorHandler);
        streamEventExecutor = new StreamListenerExecutor(channelEventExecutor.getLoop(), streamFuncHandlers);

        connections = new HashMap<>();
        streams = new HashMap<>();
        channelIds = new HashMap<>();

        server = new QuicServerExample(addr.getHostName(),port,channelEventExecutor,streamEventExecutor);
        client = new QuicClientExample(self,channelEventExecutor,streamEventExecutor);
        executor = channelEventExecutor.getLoop();

        try{
            //server.startListening(false,true);
            server.start();
        }catch (Exception e){
            throw new IOException(e);
        }
    }

    /*********************************** Stream Handlers **********************************/

    private void streamErrorHandler(QuicStreamChannel channel, Throwable throwable) {
    }

    private void streamClosedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM {} CLOSED",self,streamId);
        streams.remove(streamId);
    }

    private void streamCreatedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM CREATED {}",self,streamId);
        streams.put(streamId,channel);

    }
    public void channelRead(String channelId, byte[] bytes){
        logger.info("SELF:{} - STREAM_ID:{}",self,channelId);
        onChannelRead(channelId,bytes,channelIds.get(channelId));
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    /*********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(Channel channel, HandShakeMessage handShakeMessage){
        logger.info("{} CHANNEL ACTIVATED.",self);
        InetAddress hostName;
        int port;
        try {
            hostName = InetAddress.getByName( handShakeMessage.getHostName());
            port =handShakeMessage.getPort();
            InetSocketAddress listeningAddress = new InetSocketAddress(hostName,port);
            connections.put(listeningAddress, (QuicChannel) channel);
            channelIds.put(channel.id().asShortText(),listeningAddress);
            onChannelActive(channel,handShakeMessage,listeningAddress);
            logger.info("CONNECTION TO {} ACTIVATED.",listeningAddress);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }
    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer);

    public  void channelClosed(String channelId){
        connections.remove(channelId);
        InetSocketAddress peer = channelIds.remove(channelId);
        onChannelClosed(peer);
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
    public QuicChannel getOrThrow(String id){
        QuicChannel quicChannel = connections.get(id);
        if(quicChannel==null){
            throw new NoSuchElementException("NO SUCH ELEMENT FOUND: "+id);
        }
        return quicChannel;
    }
    public QuicStreamChannel getOrThrow2(String id){
        QuicStreamChannel streamChannel = streams.get(id);
        if(streamChannel==null){
            throw new NoSuchElementException("NO SUCH ELEMENT FOUND: "+id);
        }
        return streamChannel;
    }
    public void createStream(String parentChannelId) throws Exception {
         QuicChannel quicChannel = getOrThrow(parentChannelId);
         QuicClientExample.createStream(quicChannel,new QuicStreamReadHandler(channelEventExecutor,streamEventExecutor));
    }
    public void closeStream(String streamId){
        QuicStreamChannel quicStreamChannel = getOrThrow2(streamId);
        quicStreamChannel.close();
    }
    public void send(String streamId, byte [] message, int len){
        send(streamId,message,len, null);
    }
    public void send(String streamId,byte[] message, int len, Promise<Void> promise){
        QuicStreamChannel quicStreamChannel = getOrThrow2(streamId);
        ChannelFuture f = quicStreamChannel.writeAndFlush(Unpooled.copiedBuffer(message,0,len));
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
