package quicSupport.channels;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.*;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnknownElement;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.CustomConnection;
import quicSupport.utils.Logics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static quicSupport.utils.Logics.gson;

public abstract class CustomQuicChannel implements CustomQuicChannelConsumer {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);
    private final InetSocketAddress self;

    @Getter
    private static boolean enableMetrics;
    public final static String NAME = "QUIC_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8575";

    private final Map<InetSocketAddress, CustomConnection> connections;
    private final Map<String,InetSocketAddress> channelIds; //streamParentID, peer
    private final Map<String,InetSocketAddress> streamHostMapping;
    private final QuicClientExample client;
    private final Properties properties;
    private QuicChannelMetrics metrics;
    public CustomQuicChannel(Properties properties, boolean singleThreaded)throws IOException {
        this.properties=properties;
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        enableMetrics = properties.containsKey("metrics");
        if(enableMetrics){
            metrics=new QuicChannelMetrics(self,singleThreaded);
        }
        if(singleThreaded){
            connections = new HashMap<>();
            channelIds = new HashMap<>();
            streamHostMapping = new HashMap<>();
        }else {
            connections = new ConcurrentHashMap<>();
            channelIds = new ConcurrentHashMap<>();
            streamHostMapping = new ConcurrentHashMap<>();
        }


        QuicServerExample server = new QuicServerExample(addr.getHostName(), port, this,metrics,properties);
        client = new QuicClientExample(self,this,new NioEventLoopGroup(1), metrics);

        try{
            server.start(this::onServerSocketBind);
        }catch (Exception e){
            throw new IOException(e);
        }
    }
    public boolean enabledMetrics(){
        return enableMetrics;
    }
    /*********************************** Stream Handlers **********************************/

    public void streamErrorHandler(QuicStreamChannel channel, Throwable throwable) {
        String streamId = channel.id().asShortText();
        logger.info("{} STREAM {} ERROR: {}",self,streamId,throwable);
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        onStreamErrorHandler(peer,throwable,streamId);
    }
    public abstract void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId);

    public void streamClosedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM {} CLOSED",self,streamId);
        InetSocketAddress peer = streamHostMapping.remove(streamId);
        onStreamClosedHandler(peer,streamId);
    }

    public void streamCreatedHandler(QuicStreamChannel channel) {
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        if(peer!=null){//THE SERVER HAS NOT RECEIVED THE CLIENT'S LISTENING ADDRESS YET
            String streamId = channel.id().asShortText();
            streamHostMapping.put(streamId,peer);
            logger.info("{}. STREAM CREATED {}",self,streamId);
            connections.get(peer).addStream(channel);
            onStreamCreatedHandler(peer,streamId);
        }
    }
    public abstract void onStreamCreatedHandler(InetSocketAddress peer, String streamId);


    public void streamReader(String streamId, byte[] bytes){
        InetSocketAddress remote = streamHostMapping.get(streamId);
        CustomConnection connection = connections.get(remote);
        connection.scheduleSendHeartBeat_KeepAlive();
        //logger.info("SELF:{} - STREAM_ID:{} REMOTE:{}. RECEIVED {} DATA BYTES.",self,streamId,remote,bytes.length);
        onChannelRead(streamId,bytes,remote);
    }
    public void onKeepAliveMessage(String parentId){
        InetSocketAddress host = channelIds.get(parentId);
        connections.get(host).scheduleSendHeartBeat_KeepAlive();

    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData,InetSocketAddress remotePeer){
        boolean incoming = false;
        try {
            InetSocketAddress listeningAddress;
            QuicHandShakeMessage handShakeMessage=null;
            if(controlData==null){//is OutGoing
                listeningAddress = remotePeer;
            }else{//is InComing
                handShakeMessage = gson.fromJson(new String(controlData),QuicHandShakeMessage.class);
                listeningAddress =handShakeMessage.getAddress();
                incoming=true;
            }
            connections.put(listeningAddress, new CustomConnection(streamChannel,listeningAddress,incoming));
            channelIds.put(streamChannel.parent().id().asShortText(),listeningAddress);
            streamHostMapping.put(streamChannel.id().asShortText(),listeningAddress);

            onConnectionUp(incoming,listeningAddress);
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),incoming);
            }
            logger.info("{} CHANNEL {} TO {} ACTIVATED. INCOMING ? {}",self,streamChannel.parent().id().asShortText(),listeningAddress,incoming);
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }
    public abstract void onConnectionUp(boolean incoming, InetSocketAddress peer);

    public  void channelInactive(String channelId){
        InetSocketAddress host = channelIds.remove(channelId);
        CustomConnection connection = connections.remove(host);
        logger.info("{} CONNECTION TO {} IS DOWN.",self,connection.getRemote());
        connection.close();
        onConnectionDown(host,connection.isInComing());
    }
    public abstract void onConnectionDown(InetSocketAddress peer, boolean incoming);

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
                onOpenConnectionFailed(peer,e.getCause());
            }
        }
    }
    public void closeConnection(InetSocketAddress peer){
        CustomConnection connection = connections.get(peer);
        if(connection==null){
            logger.info("{} IS NOT CONNECTED TO {}",self,peer);
        }else{
            connection.close();
        }
    }
    private boolean isEnableMetrics(){
        if(!enableMetrics){
            Exception e = new Exception("METRICS IS NOT ENABLED!");
            e.printStackTrace();
            failedToGetMetrics(e.getCause());
        }
        return enableMetrics;
    }

    public void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler){
        if(isEnableMetrics()){
            try {
                QuicChannel connection = getOrThrow(peer).getConnection();
                handler.handle(peer,metrics.getConnectionMetrics(connection.remoteAddress()));
            } catch (Exception e) {
                failedToGetMetrics(e.getCause());
            }
        }
    }
    private CustomConnection getOrThrow(InetSocketAddress peer) throws UnknownElement {
        CustomConnection quicConnection = connections.get(peer);
        if(quicConnection==null){
            throw new UnknownElement("NO SUCH CONNECTION TO: "+peer);
        }
        return quicConnection;
    }
    public void createStream(InetSocketAddress peer) {
        try{
            CustomConnection customConnection = getOrThrow(peer);
            Logics.createStream(customConnection.getConnection(),this,metrics,customConnection.isInComing());
        }catch (Exception e){
            failedToCreateStream(peer,e.getCause());
        }
    }
    public void closeStream(String streamId){
        try{
            InetSocketAddress host = streamHostMapping.get(streamId);
            if(host==null){
                logger.debug("UNKNOWN STREAM ID: ",streamId);
            }else{
                CustomConnection connection = connections.get(host);
                connection.closeStream(streamId);
            }
        }catch (Exception e){
            failedToCloseStream(streamId,e.getCause());
        }
    }

    public void send(String streamId,byte[] message, int len) {
        InetSocketAddress host = streamHostMapping.get(streamId);
        try{
            if(host==null){
                onMessageSent(message,len,new Throwable("UNKNOWN STREAM ID: "+streamId),host);
            }else {
                send(getOrThrow(host).getStream(streamId),message,len,host);
            }
        }catch (UnknownElement e){
            logger.debug(e.getMessage());
            onMessageSent(message,len,e,host);
        }
    }
    public void send(InetSocketAddress peer,byte[] message, int len){
        try {
            send(getOrThrow(peer).getDefaultStream(),message,len,peer);
        } catch (Exception e) {
            logger.debug(e.getMessage());
            onMessageSent(message,len,e,peer);
        }
    }
    private void send(QuicStreamChannel streamChannel, byte[] message, int len, InetSocketAddress peer){
        streamChannel.writeAndFlush(Logics.writeBytes(len,message,Logics.APP_DATA))
                .addListener(future -> {
                    if(future.isSuccess()){
                        onMessageSent(message,len,null,peer);
                    }else{
                        onMessageSent(message,len,future.cause(),peer);
                    }
                });
    }

    /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/


    protected void onOutboundConnectionUp() {}


    protected void onOutboundConnectionDown() {}

    protected void onInboundConnectionUp() {}

    protected void onInboundConnectionDown() {}

    private void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }

    public void onServerSocketClose(boolean success, Throwable cause) {
        logger.debug("Server socket closed. " + (success ? "" : "Cause: " + cause));
    }
    @SneakyThrows
    public void readMetrics(QuicReadMetricsHandler handler){
        if(isEnableMetrics()){
            handler.readMetrics(metrics.currentMetrics(),metrics.oldMetrics());
        }else {
            throw new Exception("METRICS WAS NOT ENABLED!");
        }
    }
    /************************************ FAILURE HANDLERS ************************************************************/
    public abstract void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    public abstract void failedToCloseStream(String streamId, Throwable reason);
    public abstract void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer);

    public abstract void failedToCreateStream(InetSocketAddress peer, Throwable error);
    public abstract void failedToGetMetrics(Throwable cause);

    public abstract void onStreamClosedHandler(InetSocketAddress peer, String streamId);

}
