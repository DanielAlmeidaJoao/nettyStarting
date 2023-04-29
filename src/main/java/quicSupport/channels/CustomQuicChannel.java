package quicSupport.channels;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnknownElement;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.CustomConnection;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.NetworkRole;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static quicSupport.utils.QUICLogics.QUIC_METRICS;
import static quicSupport.utils.QUICLogics.gson;

public abstract class CustomQuicChannel implements CustomQuicChannelConsumer {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);

    private final InetSocketAddress self;
    private static boolean enableMetrics;
    public final static String NAME = "QUIC_CHANNEL";
    public final static String DEFAULT_PORT = "8575";
    private final Map<InetSocketAddress, CustomConnection> connections;
    private final Map<String,InetSocketAddress> channelIds; //streamParentID, peer
    private final Map<String,InetSocketAddress> streamHostMapping;
    private final Map<InetSocketAddress,List<byte []>> connecting;
    private QuicClientExample client;
    private final Properties properties;
    private QuicChannelMetrics metrics;
    public CustomQuicChannel(Properties properties, boolean singleThreaded, NetworkRole networkRole)throws IOException {
        this.properties=properties;
        InetAddress addr;
        if (properties.containsKey(QUICLogics.ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(QUICLogics.ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(QUICLogics.PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        enableMetrics = properties.containsKey(QUICLogics.QUIC_METRICS);
        if(enableMetrics){
            metrics=new QuicChannelMetrics(self,singleThreaded);
        }
        if(singleThreaded){
            connections = new HashMap<>();
            channelIds = new HashMap<>();
            streamHostMapping = new HashMap<>();
            connecting=new HashMap<>();
        }else {
            connections = new ConcurrentHashMap<>();
            channelIds = new ConcurrentHashMap<>();
            streamHostMapping = new ConcurrentHashMap<>();
            connecting=new ConcurrentHashMap<>();
        }

        if(NetworkRole.CHANNEL==networkRole||NetworkRole.SERVER==networkRole){
            try{
                new QuicServerExample(addr.getHostName(), port, this,metrics,properties)
                        .start(this::onServerSocketBind);
            }catch (Exception e){
                throw new IOException(e);
            }
        }
        if(NetworkRole.CHANNEL==networkRole||NetworkRole.CLIENT==networkRole){
            client = new QuicClientExample(self,this,new NioEventLoopGroup(1), metrics);
        }
    }
    public InetSocketAddress getSelf(){
        return self;
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
        //logger.debug("SELF:{} -- HEART BEAT RECEIVED -- {}",self,host);
        connections.get(host).scheduleSendHeartBeat_KeepAlive();
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    private void sendPendingMessages(InetSocketAddress peer){
        List<byte []> messages = connecting.remove(peer);
        if(messages!=null){
            logger.info("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",getSelf(),messages.size(),peer);
            for (byte[] message : messages) {
                send(peer,message,message.length);
            }
        }
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData,InetSocketAddress remotePeer){
        boolean inConnection = false;
        try {
            InetSocketAddress listeningAddress;
            QuicHandShakeMessage handShakeMessage=null;
            if(controlData==null){//is OutGoing
                listeningAddress = remotePeer;
            }else{//is InComing
                handShakeMessage = gson.fromJson(new String(controlData),QuicHandShakeMessage.class);
                listeningAddress = handShakeMessage.getAddress();
                inConnection=true;
            }

            CustomConnection current =  new CustomConnection(streamChannel,listeningAddress,inConnection);
            CustomConnection old = connections.put(listeningAddress,current);
            if(old!=null){
                int comp = self.toString().compareTo(listeningAddress.toString());
                if(comp==0){//CONNECTING TO ITSELF
                    connections.put(listeningAddress,old);
                    old.addStream(current.getDefaultStream());
                }else if(comp<0){//2 PEERS SIMULTANEOUSLY CONNECTING TO EACH OTHER
                    //keep the in connection
                    if(inConnection){
                        silentlyCloseCon(old.getDefaultStream());
                        logger.info("KEPT NEW STREAM {}. IN CONNECTION: {}",streamChannel.id().asShortText(),inConnection);
                    }else{
                        keepOldSilently(streamChannel, listeningAddress, old);
                        logger.info("KEPT OLD STREAM {}. IN CONNECTION: {}",old.getDefaultStream().id().asShortText(),inConnection);
                        sendPendingMessages(listeningAddress);
                        return;
                    }
                }else if(comp>0){
                    //keep the out connection
                    if(inConnection){
                        keepOldSilently(streamChannel, listeningAddress, old);
                        logger.info("KEPT OLD STREAM {}. IN CONNECTION: {}",streamChannel.id().asShortText(),inConnection);
                        sendPendingMessages(listeningAddress);
                        return;
                    }else{
                        silentlyCloseCon(old.getDefaultStream());
                        logger.info("KEPT NEW STREAM {}. IN CONNECTION: {}",streamChannel.id().asShortText(),inConnection);
                    }
                }else{
                    throw new RuntimeException("THE HASHES CANNOT BE THE SAME: "+self+" VS "+listeningAddress);
                }
            }
            channelIds.put(streamChannel.parent().id().asShortText(),listeningAddress);
            streamHostMapping.put(streamChannel.id().asShortText(),listeningAddress);
            sendPendingMessages(listeningAddress);
            onConnectionUp(inConnection,listeningAddress);
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),inConnection);
            }
            logger.info("{} CHANNEL {} TO {} ACTIVATED. INCOMING ? {}. DEFAULT STREAM: {}",self,streamChannel.parent().id().asShortText(),listeningAddress,inConnection,streamChannel.id().asShortText());
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }

    private void keepOldSilently(QuicStreamChannel streamChannel, InetSocketAddress listeningAddress, CustomConnection old) {
        connections.put(listeningAddress, old);
        streamChannel.parent().close();
    }

    private void silentlyCloseCon(QuicStreamChannel streamChannel){
        channelIds.remove(streamChannel.parent().id().asShortText());
        streamHostMapping.remove(streamChannel.id().asShortText());
        streamChannel.parent().close();
    }
    public abstract void onConnectionUp(boolean incoming, InetSocketAddress peer);

    public  void channelInactive(String channelId){
        try{
            InetSocketAddress host = channelIds.remove(channelId);
            if(host!=null){
                CustomConnection connection = connections.remove(host);
                logger.info("{} CONNECTION TO {} IS DOWN.",self,connection.getRemote());
                connection.close();
                onConnectionDown(host,connection.isInComing());
                System.out.println("CHANNEL TO "+host+" INACTIVE "+connections.size());
            }
        }catch (Exception e){
            logger.debug(e.getLocalizedMessage());
        }
    }
    public abstract void onConnectionDown(InetSocketAddress peer, boolean incoming);

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/

    public void open(InetSocketAddress peer) {

        if(connections.containsKey(peer)){
            logger.info("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            if(connecting.containsKey(peer)){
                return;
            }else{
                connecting.put(peer,new LinkedList<>());
            }
            logger.info("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer,properties);
            }catch (Exception e){
                e.printStackTrace();
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
            QUICLogics.createStream(customConnection.getConnection(),this,metrics,customConnection.isInComing());
        }catch (Exception e){
            failedToCreateStream(peer,e.getCause());
        }
    }
    public void closeStream(String streamId){
        try{
            InetSocketAddress host = streamHostMapping.get(streamId);
            if(host==null){
                logger.debug("UNKNOWN STREAM ID: {}",streamId);
            }else{
                CustomConnection connection = connections.get(host);
                connection.closeStream(streamId);
            }
        }catch (Exception e){
            failedToCloseStream(streamId,e.getCause());
        }
    }

    public void send(String streamId, byte[] message, int len) {
        InetSocketAddress host = streamHostMapping.get(streamId);
        try{
            if(host==null){
                onMessageSent(message,len,new Throwable("UNKNOWN STREAM ID: "+streamId),host);
            }else {
                sendMessage(getOrThrow(host).getStream(streamId),message,len,host);
            }
        }catch (UnknownElement e){
            logger.debug(e.getMessage());
            onMessageSent(message,len,e,host);
        }
    }
    public void send(InetSocketAddress peer, byte[] message, int len){
        List<byte []> pendingMessages = connecting.get(peer);
        if( pendingMessages !=null ){
            pendingMessages.add(message);
            logger.info("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            return;
        }
        try {
            sendMessage(getOrThrow(peer).getDefaultStream(),message,len,peer);
        } catch (Exception e) {
            //e.printStackTrace();
            //logger.info(e.getMessage());
            //System.out.println(e.getMessage()+" CONNECTIONSS "+connections.size());
            onMessageSent(message,len,e,peer);
        }
    }
    private void sendMessage(QuicStreamChannel streamChannel, byte[] message, int len, InetSocketAddress peer){
        streamChannel.writeAndFlush(QUICLogics.writeBytes(len,message, QUICLogics.APP_DATA))
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
    private void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }

    public void onServerSocketClose(boolean success, Throwable cause) {
        logger.debug("Server socket closed. " + (success ? "" : "Cause: " + cause));
    }

    public void readMetrics(QuicReadMetricsHandler handler){
        if(isEnableMetrics()){
            handler.readMetrics(metrics.currentMetrics(),metrics.oldMetrics());
        }else {
            //throw new Exception("METRICS WAS NOT ENABLED!");
            logger.info("METRICS WAS NOT ENABLED! ADD PROPERTY {}=TRUE TO ENABLE IT",QUIC_METRICS);
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
