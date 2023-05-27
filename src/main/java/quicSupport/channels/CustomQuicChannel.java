package quicSupport.channels;

import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnknownElement;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.handlers.pipeline.QUICRawStreamDecoder;
import quicSupport.handlers.pipeline.QuicDelimitedMessageDecoder;
import quicSupport.handlers.pipeline.QuicMessageEncoder;
import quicSupport.handlers.pipeline.QuicUnstructuredStreamEncoder;
import quicSupport.utils.CustomConnection;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.ConnectionOrStreamType;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static quicSupport.utils.QUICLogics.*;

public class CustomQuicChannel implements CustomQuicChannelConsumer, CustomQuicChannelInterface {
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
    private QuicServerExample server;
    private final Properties properties;
    private final boolean withHeartBeat;
    private QuicChannelMetrics metrics;
    private static long heartBeatTimeout;
    private final boolean connectIfNotConnected;
    private final ChannelHandlerMethods overridenMethods;
    public CustomQuicChannel(Properties properties, boolean singleThreaded, NetworkRole networkRole, ChannelHandlerMethods mom)throws IOException {
        this.properties=properties;
        this.overridenMethods = mom;
        InetAddress addr;
        if (properties.containsKey(QUICLogics.ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(QUICLogics.ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(QUICLogics.PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        enableMetrics = properties.containsKey(QUICLogics.QUIC_METRICS);
        withHeartBeat = properties.get(QUICLogics.WITH_HEART_BEAT)!=null;
        heartBeatTimeout = Long.parseLong(properties.getProperty(MAX_IDLE_TIMEOUT_IN_SECONDS,maxIdleTimeoutInSeconds+""));
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
                server = new QuicServerExample(addr.getHostName(), port, this,metrics,properties);
                        server.start(this::onServerSocketBind);
            }catch (Exception e){
                throw new IOException(e);
            }
        }
        if(NetworkRole.CHANNEL==networkRole||NetworkRole.CLIENT==networkRole){
            if(NetworkRole.CLIENT==networkRole){
                properties.remove(CONNECT_ON_SEND);
            }
            client = new QuicClientExample(self,this,new NioEventLoopGroup(1), metrics);
        }
        connectIfNotConnected = properties.getProperty(CONNECT_ON_SEND)!=null;
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
        overridenMethods.onStreamErrorHandler(peer,throwable,streamId);
    }

    public void streamClosedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM {} CLOSED",self,streamId);
        InetSocketAddress peer = streamHostMapping.remove(streamId);
        if(peer!=null){
            overridenMethods.onStreamClosedHandler(peer,streamId);
        }
    }

    public void streamCreatedHandler(QuicStreamChannel channel) {
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        if(peer!=null){//THE FIRST STREAM IS DEFAULT. NOT NOTIFIED TO THE CLIENT
            String streamId = channel.id().asShortText();
            streamHostMapping.put(streamId,peer);
            logger.info("{}. STREAM CREATED {}",self,streamId);
            connections.get(peer).addStream(channel);
            overridenMethods.onStreamCreatedHandler(peer,streamId);
        }
    }


    public void onReceivedDelimitedMessage(String streamId, byte[] bytes){
        InetSocketAddress remote = streamHostMapping.get(streamId);
        if(remote==null){return;}
        CustomConnection connection = connections.get(remote);
        if(connection!=null){
            if(withHeartBeat){connection.scheduleSendHeartBeat_KeepAlive();}
            //logger.info("SELF:{} - STREAM_ID:{} REMOTE:{}. RECEIVED {} DATA BYTES.",self,streamId,remote,bytes.length);
            overridenMethods.onChannelReadDelimitedMessage(streamId,bytes,remote);
        }
    }
    public void onReceivedStream(String streamId, byte [] bytes){
        System.out.println("RECEIVED STREAMID BYTES:"+bytes.length);
        InetSocketAddress remote = streamHostMapping.get(streamId);
        if(remote==null){return;}
        CustomConnection connection = connections.get(remote);
        if(connection!=null){
            overridenMethods.onChannelReadFlowStream(streamId,bytes,remote);
        }
    }

    public void onKeepAliveMessage(String parentId){
        InetSocketAddress host = channelIds.get(parentId);
        logger.info("SELF:{} -- HEART BEAT RECEIVED -- {}",self,host);
        if(host!=null){
            CustomConnection connection = connections.get(host);
            if(connection!=null&&withHeartBeat){
                connection.scheduleSendHeartBeat_KeepAlive();
            }
        }
    }

    private void sendPendingMessages(InetSocketAddress peer){
        List<byte []> messages = connecting.remove(peer);
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",getSelf(),messages.size(),peer);
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
            QuicHandShakeMessage handShakeMessage;
            if(controlData==null){//is OutGoing
                listeningAddress = remotePeer;
            }else{//is InComing
                handShakeMessage = gson.fromJson(new String(controlData),QuicHandShakeMessage.class);
                listeningAddress = handShakeMessage.getAddress();
                System.out.println("BEFORE");
                for (Map.Entry<String, ChannelHandler> stringChannelHandlerEntry : streamChannel.pipeline()) {
                    System.out.println(stringChannelHandlerEntry.getKey()+" "+stringChannelHandlerEntry.getValue());
                }
                if(handShakeMessage.connectionOrStreamType== ConnectionOrStreamType.UNSTRUCTURED_STREAM){
                    streamChannel.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                    streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,true));
                }
                System.out.println("AFTER");
                for (Map.Entry<String, ChannelHandler> stringChannelHandlerEntry : streamChannel.pipeline()) {
                    System.out.println(stringChannelHandlerEntry.getKey()+" "+stringChannelHandlerEntry.getValue());
                }
                inConnection=true;
            }

            CustomConnection current =  new CustomConnection(streamChannel,listeningAddress,inConnection,withHeartBeat,heartBeatTimeout);
            CustomConnection old = connections.put(listeningAddress,current);
            if(old!=null){
                int comp = QUICLogics.compAddresses(self,listeningAddress);
                if(comp==0){//CONNECTING TO ITSELF
                    connections.put(listeningAddress,old);
                    old.addStream(current.getDefaultStream());
                }else {
                    if(old.hasPassedOneSec()){
                        silentlyCloseCon(old.getDefaultStream());
                        logger.info("KEPT NEW STREAM {}. IN CONNECTION: {} FROM {}",streamChannel.id().asShortText(),inConnection,listeningAddress);
                    }else{
                        if(comp<0){//2 PEERS SIMULTANEOUSLY CONNECTING TO EACH OTHER
                            //keep the in connection
                            if(inConnection){
                                silentlyCloseCon(old.getDefaultStream());
                                logger.info("KEPT NEW STREAM {}. IN CONNECTION: {} FROM {}",streamChannel.id().asShortText(),inConnection,listeningAddress);
                            }else{
                                keepOldSilently(streamChannel, listeningAddress, old);
                                logger.info("KEPT OLD STREAM {}. IN CONNECTION: {} FROM {}",old.getDefaultStream().id().asShortText(),inConnection,listeningAddress);
                                sendPendingMessages(listeningAddress);
                                return;
                            }
                        }else if(comp>0){
                            //keep the out connection
                            if(inConnection){
                                keepOldSilently(streamChannel, listeningAddress, old);
                                logger.info("KEPT OLD STREAM {}. IN CONNECTION: {} FROM {}",streamChannel.id().asShortText(),inConnection,listeningAddress);
                                sendPendingMessages(listeningAddress);
                                return;
                            }else{
                                silentlyCloseCon(old.getDefaultStream());
                                logger.info("KEPT NEW STREAM {}. IN CONNECTION: {} FROM {}",streamChannel.id().asShortText(),inConnection,listeningAddress);
                            }
                        }else{
                            throw new RuntimeException("THE HASHES CANNOT BE THE SAME: "+self+" VS "+listeningAddress);
                        }
                    }
                }
            }
            channelIds.put(streamChannel.parent().id().asShortText(),listeningAddress);
            streamHostMapping.put(streamChannel.id().asShortText(),listeningAddress);
            sendPendingMessages(listeningAddress);
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),inConnection);
            }
            overridenMethods.onConnectionUp(inConnection,listeningAddress);
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

    public  void channelInactive(String channelId){
        try{
            InetSocketAddress host = channelIds.remove(channelId);
            if(host!=null){
                CustomConnection connection = connections.remove(host);
                logger.info("{} CONNECTION TO {} IS DOWN.",self,connection.getRemote());
                connection.close();
                overridenMethods.onConnectionDown(host,connection.isInComing());
            }
        }catch (Exception e){
            logger.debug(e.getLocalizedMessage());
        }
    }

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/

    public void open(InetSocketAddress peer,ConnectionOrStreamType type) {
        openLogics(peer,type);
    }
    private void openLogics(InetSocketAddress peer,ConnectionOrStreamType connectionOrStreamType){
        if(connections.containsKey(peer)){
            logger.debug("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            if(connecting.containsKey(peer)){
                return;
            }else{
                connecting.put(peer,new LinkedList<>());
            }
            logger.info("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer,properties,connectionOrStreamType);
            }catch (Exception e){
                e.printStackTrace();
                handleOpenConnectionFailed(peer,e.getCause());
            }
        }
    }
    public void closeConnection(InetSocketAddress peer){
        //System.out.println("CLOSING THIS CONNECTION: "+peer);
        CustomConnection connection = connections.get(peer);
        if(connection==null){
            logger.debug("{} IS NOT CONNECTED TO {}",self,peer);
        }else{
            connection.close();
        }
    }
    private boolean isEnableMetrics(){
        if(!enableMetrics){
            Exception e = new Exception("METRICS IS NOT ENABLED!");
            e.printStackTrace();
            overridenMethods.failedToGetMetrics(e.getCause());
        }
        return enableMetrics;
    }

    public void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler){
        if(isEnableMetrics()){
            try {
                QuicChannel connection = getOrThrow(peer).getConnection();
                handler.handle(peer,metrics.getConnectionMetrics(connection.remoteAddress()));
            } catch (Exception e) {
                overridenMethods.failedToGetMetrics(e.getCause());
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
            overridenMethods.failedToCreateStream(peer,e.getCause());
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
            overridenMethods.failedToCloseStream(streamId,e.getCause());
        }
    }

    public void send(String streamId, byte[] message, int len) {
        InetSocketAddress host = streamHostMapping.get(streamId);
        try{
            if(host==null){
                overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN STREAM ID: "+streamId),host);
            }else {
                sendMessage(getOrThrow(host).getStream(streamId),message,len,host);
            }
        }catch (UnknownElement e){
            logger.debug(e.getMessage());
            overridenMethods.onMessageSent(message,len,e,host);
        }
    }
    public void send(InetSocketAddress peer, byte[] message, int len){
        CustomConnection connection = connections.get(peer);
        if(connection==null){
            List<byte []> pendingMessages = connecting.get(peer);
            if( pendingMessages !=null ){
                pendingMessages.add(message);
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if (connectIfNotConnected){
                openLogics(peer,ConnectionOrStreamType.STRUCTURED_MESSAGE);
                connecting.get(peer).add(message);
            }else{
                overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN CONNECTION TO "+peer),peer);
            }
        }else{
            sendMessage(connection.getDefaultStream(),message,len,peer);
        }
    }
    private void sendMessage(QuicStreamChannel streamChannel, byte[] message, int len, InetSocketAddress peer){
        streamChannel.writeAndFlush(QUICLogics.writeBytes(len,message, QUICLogics.APP_DATA))
                .addListener(future -> {
                    if(future.isSuccess()){
                        overridenMethods.onMessageSent(message,len,null,peer);
                    }else{
                        overridenMethods.onMessageSent(message,len,future.cause(),peer);
                    }
                });
    }
    public boolean isConnected(InetSocketAddress peer){
        return connections.containsKey(peer);
    }
    public final String [] getStreams(){
        return streamHostMapping.keySet().toArray(new String[streamHostMapping.size()]);
    }
    public final InetSocketAddress [] getConnections(){
        return connections.keySet().toArray(new InetSocketAddress[connections.size()]);
    }
    public final int connectedPeers(){
        return connections.size();
    }

    @Override
    public void shutDown() {
        if(server!=null){
            server.closeServer();
        }
        if(client!=null){
            client.closeClient();
        }
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
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause){
        connecting.remove(peer);
        overridenMethods.onOpenConnectionFailed(peer,cause);
    }


}
