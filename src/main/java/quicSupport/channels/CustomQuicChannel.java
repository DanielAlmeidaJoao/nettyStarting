package quicSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.Exceptions.UnknownElement;
import quicSupport.client_server.QuicClientExample;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.handlers.pipeline.*;
import quicSupport.utils.ConnectionId;
import quicSupport.utils.CustomConnection;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static quicSupport.utils.QUICLogics.*;

public class CustomQuicChannel implements CustomQuicChannelConsumer, CustomQuicChannelInterface {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);

    private final InetSocketAddress self;
    private static boolean enableMetrics;
    public final static String NAME = "QUIC_CHANNEL";
    public final static String DEFAULT_PORT = "8575";
    private final Map<InetSocketAddress, CustomConnection> connections;
    private final Map<String,InetSocketAddress> streamHostMapping;
    private final Map<String,Pair<InetSocketAddress,List<Pair<byte [],Integer>>>> connecting;

    private QuicClientExample client;
    private QuicServerExample server;
    private final Properties properties;
    private final boolean withHeartBeat;
    private QuicChannelMetrics metrics;
    private static long heartBeatTimeout;
    private final boolean connectIfNotConnected;
    private final ChannelHandlerMethods overridenMethods;
    private final AtomicInteger idCounterGenerator;
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
            streamHostMapping = new HashMap<>();
            connecting=new HashMap<>();
        }else {
            connections = new ConcurrentHashMap<>();
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
        idCounterGenerator = new AtomicInteger();
        connectIfNotConnected = properties.getProperty(CONNECT_ON_SEND)!=null;
    }
    public InetSocketAddress getSelf(){
        return self;
    }
    public boolean enabledMetrics(){
        return enableMetrics;
    }
    /*********************************** Stream Handlers **********************************/

    public void streamErrorHandler(ConnectionId connectionId, Throwable throwable) {
        logger.info("{} STREAM {} ERROR: {}",self,connectionId.linkId,throwable);
        overridenMethods.onStreamErrorHandler(connectionId.address,throwable,connectionId.linkId);
    }

    public void streamInactive(ConnectionId connectionId) {
        logger.info("{}. STREAM {} CLOSED",self,connectionId.linkId);
        streamHostMapping.remove(connectionId.linkId);
        overridenMethods.onStreamClosedHandler(connectionId.address,connectionId.linkId);
    }

    public void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, Triple<Short,Short,Short> triple, ConnectionId identification, boolean inConnection) {
        streamHostMapping.put(identification.linkId,identification.address);
        logger.info("{}. STREAM CREATED {}",self,identification);
        connections.get(identification.address).addStream(identification,channel);
        overridenMethods.onStreamCreatedHandler(identification,type,triple);
        overridenMethods.onConnectionUp(inConnection,identification,type);
    }
    public void onReceivedDelimitedMessage(ConnectionId streamId, byte[] bytes){
        CustomConnection connection = connections.get(streamId.address);
        if(connection!=null){
            if(withHeartBeat){connection.scheduleSendHeartBeat_KeepAlive();}
            //logger.info("SELF:{} - STREAM_ID:{} REMOTE:{}. RECEIVED {} DATA BYTES.",self,streamId,remote,bytes.length);
            overridenMethods.onChannelReadDelimitedMessage(streamId,bytes);
        }
    }
    public void onReceivedStream(ConnectionId streamId, byte [] bytes){
        InetSocketAddress remote = streamHostMapping.get(streamId);
        if(remote==null){return;}
        CustomConnection connection = connections.get(remote);
        if(connection!=null){
            overridenMethods.onChannelReadFlowStream(streamId,bytes);
        }
    }

    public void onKeepAliveMessage(ConnectionId parentId){
        CustomConnection connection = connections.get(parentId.address);
        logger.info("SELF:{} -- HEART BEAT RECEIVED -- {}",self,parentId.address);
        if(connection!=null){
            if(connection!=null&&withHeartBeat){
                connection.scheduleSendHeartBeat_KeepAlive();
            }
        }
    }

    private void sendPendingMessages(ConnectionId connectionId, TransmissionType type){
        List<Pair<byte [],Integer>> messages = connecting.remove(connectionId.linkId).getRight();
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",getSelf(),messages.size(),connectionId);
            for (Pair<byte[],Integer> message : messages) {
                send(connectionId.address,message.getLeft(),message.getRight(),type);
            }
        }
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, QuicHandShakeMessage handShakeMessage, ConnectionId id, TransmissionType type){
        boolean inConnection = false;
        try {
            if(handShakeMessage!=null){//is inConnection
                type = handShakeMessage.transmissionType;
                inConnection=true;
                QuicServerChannelConHandler.setId(streamChannel.parent(),id);
                ((QuicStreamHandler) streamChannel.pipeline().get(QuicStreamHandler.HANDLER_NAME)).setId(id);;
            }
            if(TransmissionType.UNSTRUCTURED_STREAM==type){
                streamChannel.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,id));
            }
            CustomConnection current =  new CustomConnection(streamChannel,id,inConnection,withHeartBeat,heartBeatTimeout,type);
            CustomConnection broCon = connections.get(id.address);
            if(broCon == null ){
                connections.put(id.address,current);
            }else{
                broCon.addConnection(current);
            }
            streamHostMapping.put(id.linkId,id.address);
            if(!inConnection){
                sendPendingMessages(id,type);
            }
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),id.address,streamChannel.parent().collectStats().get(),inConnection);
            }
            overridenMethods.onConnectionUp(inConnection,id,type);
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }

    public  void channelInactive(ConnectionId connectionId){
        CustomConnection connection = connections.remove(connectionId.address);
        logger.info("{} CONNECTION TO {} IS DOWN.",self,connectionId.address);
        if(connection != null){
            streamHostMapping.remove(connectionId.linkId);
            //closing the default to the peer
            if(connection.getConnectionId().linkId==connectionId.linkId){
                CustomConnection c = connection.broReplacer();
                if(c!=null){
                    connections.put(connectionId.address,c);
                }
            }else{
                connection.removeBro(connectionId.linkId);
                streamHostMapping.remove(connectionId.linkId);
            }
            connection.close();
        }
    }

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/

    public String nextId(){
        return "quic_link_"+idCounterGenerator.getAndIncrement();
    }

    public String open(InetSocketAddress peer, TransmissionType type) {
        return openLogics(peer,type,null);
    }

    public String openLogics(InetSocketAddress peer, TransmissionType transmissionType, String conId){
        if(conId == null){
            conId = nextId();
        }
        if(connections.containsKey(peer)){
            short p = -1;
            connecting.put(conId,Pair.of(peer,new LinkedList<>()));
            baseCreateStream(peer,transmissionType,Triple.of(p,p,p),conId);
            return conId;
        }
        connecting.put(conId,Pair.of(peer,new LinkedList<>()));
        logger.info("{} CONNECTING TO {}",self,peer);
        try {
            client.connect(peer,properties, transmissionType, conId);
        }catch (Exception e){
            System.out.println("ddasdsa FAILED ++++++++++++++++++++");
            e.printStackTrace();
            System.out.println("FAILED ++++++++++++++++++++");
            handleOpenConnectionFailed(ConnectionId.of(peer,conId),e.getCause());
        }
        return conId;
    }
    public void closeConnection(InetSocketAddress peer){
        //System.out.println("CLOSING THIS CONNECTION: "+peer);
        CustomConnection connection = connections.remove(peer);
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
    public String createStream(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args) {
        return baseCreateStream(peer,type, args,null);
    }
    public String baseCreateStream(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args, String conId) {
        if(conId == null){
            conId = nextId();
        }
        final ConnectionId id = ConnectionId.of(peer,conId);
        try{
            CustomConnection customConnection = getOrThrow(peer);
            QuicStreamChannel quicStreamChannel = QUICLogics.createStream(customConnection.getConnection(),this,metrics,id);
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeInt(type.ordinal());
            if(TransmissionType.UNSTRUCTURED_STREAM==type){
                byteBuf.writeShort(args.getLeft());
                byteBuf.writeShort(args.getMiddle());
                byteBuf.writeShort(args.getRight());
            }
            byte [] data = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(data);
            byteBuf.release();
            quicStreamChannel.writeAndFlush(QUICLogics.writeBytes(data.length,data,STREAM_CREATED, TransmissionType.STRUCTURED_MESSAGE))
                    .addListener(future -> {
                        if(future.isSuccess()){
                            if(TransmissionType.UNSTRUCTURED_STREAM == type){
                                quicStreamChannel.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                                quicStreamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,id));
                            }
                            streamCreatedHandler(quicStreamChannel,type,args,id,false);
                        }else{
                            //future.cause().printStackTrace();
                            quicStreamChannel.close();
                            quicStreamChannel.disconnect();
                            quicStreamChannel.shutdown();
                        }
                    });
        }catch (Exception e){
            overridenMethods.failedToCreateStream(peer,e.getCause());
        }
        return id.linkId;
    }
    public void closeStream(String streamId){
        try{
            InetSocketAddress host = streamHostMapping.remove(streamId);
            if(host==null){
                logger.debug("UNKNOWN LINK ID: {}",streamId);
            }else{
                CustomConnection connection = connections.remove(host);
                CustomConnection boss = connection.closeStream(streamId);
                if(boss != null){
                    connections.put(boss.getConnectionId().address,boss);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            overridenMethods.failedToCloseStream(streamId,e.getCause());
        }
    }

    public void send(String streamId, byte[] message, int len, TransmissionType type) {
        InetSocketAddress host = streamHostMapping.get(streamId);
        try{
            if(host==null){
                overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN STREAM ID: "+streamId),host, type);
            }else {
                sendMessage(getOrThrow(host).getStream(streamId),message,len,host,type);
            }
        }catch (UnknownElement e){
            logger.debug(e.getMessage());
            overridenMethods.onMessageSent(message,len,e,host, type);
        }
    }
    public void send(InetSocketAddress peer, byte[] message, int len, TransmissionType type){
        CustomConnection connection = connections.get(peer);
        if(connection==null){
            List<Pair<byte [],Integer>> pendingMessages = connecting.get(peer).getRight();
            if( pendingMessages !=null ){
                pendingMessages.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if (connectIfNotConnected){
                String conId = openLogics(peer,type,null);
                connecting.get(peer).getRight().add(Pair.of(message,len));
            }else{
                overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN CONNECTION TO "+peer),peer, type);
            }
        }else{
            sendMessage(connection.getDefaultStream(),message,len,peer,type);
        }
    }
    private void sendMessage(QuicStreamChannel streamChannel, byte[] message, int len, InetSocketAddress peer, TransmissionType type){
        streamChannel.writeAndFlush(QUICLogics.writeBytes(len,message, QUICLogics.APP_DATA,type))
                .addListener(future -> {
                    if(future.isSuccess()){
                        overridenMethods.onMessageSent(message,len,null,peer,type);
                    }else{
                        overridenMethods.onMessageSent(message,len,future.cause(),peer,type);
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

    @Override
    public TransmissionType getConnectionType(InetSocketAddress peer) {
        CustomConnection connection = connections.get(peer);
        if(connection==null){
            throw new NoSuchElementException("UNKNOWN ELEMENT "+peer);
        }
        return connection.transmissionType;
    }

    @Override
    public TransmissionType getConnectionType(String streamId) {
        try{
            InetSocketAddress peer = streamHostMapping.get(streamId);
            CustomConnection connection = connections.get(peer);
            QuicStreamChannel channel = connection.getStream(streamId);
            if(channel.pipeline().get(QuicUnstructuredStreamEncoder.HANDLER_NAME)==null){
                return TransmissionType.STRUCTURED_MESSAGE;
            }else{
                return TransmissionType.UNSTRUCTURED_STREAM;
            }
        }catch (Exception e){
            throw new NoSuchElementException("UNKNOWN STREAM "+streamId);
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
    public void handleOpenConnectionFailed(ConnectionId id, Throwable cause){
        connecting.remove(id.address);
        overridenMethods.onOpenConnectionFailed(id.address,cause);
    }


}
