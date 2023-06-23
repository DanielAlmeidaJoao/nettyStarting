package quicSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.AttributeKey;
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
import quicSupport.utils.customConnections.CustomQUICConnection;
import quicSupport.utils.customConnections.CustomQUICStreamCon;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.entities.QUICConnectingOBJ;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import static quicSupport.utils.QUICLogics.*;

public class CustomQuicChannel implements CustomQuicChannelConsumer, CustomQuicChannelInterface {
    private static final Logger logger = LogManager.getLogger(CustomQuicChannel.class);

    private final InetSocketAddress self;
    private static boolean enableMetrics;
    public final static String NAME = "QUIC_CHANNEL";
    public final static String DEFAULT_PORT = "8575";
    private final Map<InetSocketAddress,List<CustomQUICConnection>> addressToQUICCons;
    private final Map<String, CustomQUICStreamCon> nettyIdToStream; //streamParentID, peer
    private final Map<String,CustomQUICStreamCon> customStreamIdToStream;
    private final Map<InetSocketAddress, QUICConnectingOBJ> connecting;

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
        addressToQUICCons = TCPStreamUtils.getMapInst(singleThreaded);
        nettyIdToStream = TCPStreamUtils.getMapInst(singleThreaded);
        customStreamIdToStream = TCPStreamUtils.getMapInst(singleThreaded);
        connecting= TCPStreamUtils.getMapInst(singleThreaded);

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
        CustomQUICStreamCon streamCon = nettyIdToStream.get(streamId);
        if(streamCon!=null){
            overridenMethods.onStreamErrorHandler(streamCon.customQUICConnection.getRemote(), throwable,streamId);
        }
    }

    public String nextId(){
        return "quicchan"+ TCPStreamUtils.channelIdCounter.getAndIncrement();
    }
    private void removeConnection(CustomQUICConnection streamCon){
        List<CustomQUICConnection> con = addressToQUICCons.get(streamCon.getRemote());
        if(con!=null){
            for (CustomQUICConnection customQUICConnection : con) {
                if(customQUICConnection.getConnection()==streamCon){
                    con.remove(customQUICConnection);
                    return;
                }
            }
        }
    }
    private void addStream(CustomQUICStreamCon streamCon){
        List<CustomQUICConnection> con = addressToQUICCons.get(streamCon.customQUICConnection.getRemote());
        if(con!=null){
            for (CustomQUICConnection customQUICConnection : con) {
                if(customQUICConnection.getConnection()==streamCon.streamChannel.parent()){
                    customQUICConnection.addStream(streamCon);
                    return;
                }
            }
        }
    }
    private CustomQUICConnection getCustomQUICConnection(InetSocketAddress inetSocketAddress){
        List<CustomQUICConnection> con = addressToQUICCons.get(inetSocketAddress);
        if(con!=null && !con.isEmpty()){
            return con.get(0);
        }
        return null;
    }
    private void closeConnections(List<CustomQUICConnection> cons){
        for (CustomQUICConnection con : cons) {
            con.close();
        }
    }
    public void streamInactiveHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM {} CLOSED",self,streamId);
        CustomQUICStreamCon streamCon = nettyIdToStream.remove(streamId);
        if(streamCon!=null){
            customStreamIdToStream.remove(streamCon.customStreamId);
            streamCon.customQUICConnection.closeStream(streamId);
            overridenMethods.onStreamClosedHandler(streamCon.customQUICConnection.getRemote(),streamCon.customStreamId);
        }
    }
    public void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, Triple<Short,Short,Short> triple, String customId, boolean inConnection) {
        logger.info("{}. STREAM CREATED {}",self,customId);
        CustomQUICStreamCon parent = nettyIdToStream.get(channel.parent().id().asShortText());
        CustomQUICStreamCon con = new CustomQUICStreamCon(channel,customId,type,parent.customQUICConnection);
        con.customQUICConnection.addStream(con);
        customStreamIdToStream.put(customId,con);
        nettyIdToStream.put(channel.id().asShortText(),con);
        sendPendingMessages(con.customQUICConnection.getRemote(), type,channel.id().asShortText());
        overridenMethods.onConnectionUp(inConnection,con.customQUICConnection.getRemote(), type,customId);
    }




    public void onReceivedDelimitedMessage(String streamId, byte[] bytes){
        CustomQUICStreamCon streamCon = nettyIdToStream.get(streamId);
        if(streamCon!=null){
            if(withHeartBeat){streamCon.customQUICConnection.scheduleSendHeartBeat_KeepAlive();}
            //logger.info("SELF:{} - STREAM_ID:{} REMOTE:{}. RECEIVED {} DATA BYTES.",self,streamId,remote,bytes.length);
            overridenMethods.onChannelReadDelimitedMessage(streamCon.customStreamId,bytes,streamCon.customQUICConnection.getRemote());
        }
    }
    public void onReceivedStream(String streamId, byte [] bytes){
        CustomQUICStreamCon streamCon = nettyIdToStream.get(streamId);
        if(streamCon != null){
            overridenMethods.onChannelReadFlowStream(streamCon.customStreamId,bytes,streamCon.customQUICConnection.getRemote());
        }
    }

    public void onKeepAliveMessage(String parentId){
        CustomQUICStreamCon streamCon = nettyIdToStream.get(parentId);
        if(streamCon!=null){
            InetSocketAddress remote = streamCon.customQUICConnection.getRemote();
            logger.info("SELF:{} -- HEART BEAT RECEIVED -- {}",self,remote);
            if(withHeartBeat){
                streamCon.customQUICConnection.scheduleSendHeartBeat_KeepAlive();
            }
        }
    }

    private void sendPendingMessages(InetSocketAddress peer, TransmissionType type, String streamId){
        QUICConnectingOBJ QUICConnectingOBJ = connecting.remove(peer);
        if(QUICConnectingOBJ == null){
            return;
        }
        List<Pair<byte[],Integer>> messages = QUICConnectingOBJ.msgWithLen;
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",getSelf(),messages.size(),peer);
            for (Pair<byte[],Integer> message : messages) {
                send(peer,message.getLeft(),message.getRight(),type);
            }
        }
        if(QUICConnectingOBJ.connectionsToOpen!=null){
            short p = -1;
            for (Pair<String,TransmissionType> customConId : QUICConnectingOBJ.connectionsToOpen) {
                createStreamLogics(peer,customConId.getRight(),Triple.of(p,p,p),customConId.getLeft());
            }
        }
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData, InetSocketAddress remotePeer, TransmissionType type){
        boolean inConnection = false;
        try {
            InetSocketAddress listeningAddress;
            QuicHandShakeMessage handShakeMessage;
            String customConId;
            if(controlData==null){//is OutGoing
                listeningAddress = remotePeer;
                customConId = streamChannel.parent().attr(AttributeKey.valueOf(TCPStreamUtils.CUSTOM_ID_KEY)).toString();
            }else{//is InComing
                handShakeMessage = gson.fromJson(new String(controlData),QuicHandShakeMessage.class);
                listeningAddress = handShakeMessage.getAddress();
                type = handShakeMessage.transmissionType;
                inConnection=true;
                customConId = nextId();
            }
            if(TransmissionType.UNSTRUCTURED_STREAM==type){
                streamChannel.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,inConnection));
            }

            CustomQUICStreamCon quicStreamChannel = new CustomQUICStreamCon(streamChannel,customConId, type,null);
            CustomQUICConnection current =  new CustomQUICConnection(quicStreamChannel, listeningAddress,inConnection,withHeartBeat,heartBeatTimeout,type);
            quicStreamChannel.customQUICConnection = current;
            //use lock here
            List<CustomQUICConnection> connections = addressToQUICCons.get(listeningAddress);
            if(connections == null){
                connections = new LinkedList<>();
                addressToQUICCons.put(listeningAddress,connections);
            }
            // till here
            connections.add(current);
            nettyIdToStream.put(streamChannel.parent().id().asShortText(),quicStreamChannel);
            nettyIdToStream.put(streamChannel.id().asShortText(),quicStreamChannel);
            customStreamIdToStream.put(customConId,quicStreamChannel);
            sendPendingMessages(listeningAddress,type,streamChannel.id().asShortText());
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),inConnection);
            }
            overridenMethods.onConnectionUp(inConnection,listeningAddress,type,customConId);
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }

    public  void channelInactive(String parentId){
        CustomQUICConnection customQUICConnection = nettyIdToStream.remove(parentId).customQUICConnection;
        List<CustomQUICConnection> connections = addressToQUICCons.get(customQUICConnection.getRemote());
        if(connections !=null && connections.remove(customQUICConnection) && connections.isEmpty()){
            addressToQUICCons.remove(customQUICConnection.getRemote());
        }
        logger.info("{} CONNECTION TO {} IS DOWN.",self,customQUICConnection.getRemote());
    }

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/

    public String open(InetSocketAddress peer, TransmissionType type) {
        return openLogics(peer,type,null);
    }
    public String openLogics(InetSocketAddress peer, TransmissionType transmissionType, String id){
        if(id == null){
            id = nextId();
        }
        QUICConnectingOBJ QUICConnectingOBJ = connecting.get(peer);
        if(QUICConnectingOBJ != null){
            QUICConnectingOBJ.addToQueue(id,transmissionType);
            return id;
        }else if(addressToQUICCons.containsKey(peer)){
            Short p = -1;
            return createStreamLogics(peer,transmissionType,Triple.of(p,p,p),id);
        }
        connecting.put(peer,new QUICConnectingOBJ(id,peer));
        logger.info("{} CONNECTING TO {}",self,peer);
        try {
            client.connect(peer,properties, transmissionType,id);
        }catch (Exception e){
            e.printStackTrace();
            handleOpenConnectionFailed(peer,e.getCause());
        }
        return id;
    }
    public void closeConnection(InetSocketAddress peer){
        //System.out.println("CLOSING THIS CONNECTION: "+peer);
        List<CustomQUICConnection> connections = addressToQUICCons.remove(peer);
        if(connections==null){
            logger.debug("{} IS NOT CONNECTED TO {}",self,peer);
        }else{
            closeConnections(connections);
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
    private CustomQUICConnection getOrThrow(InetSocketAddress peer) throws UnknownElement {
        List<CustomQUICConnection> connections = addressToQUICCons.get(peer);
        if(connections==null || connections.isEmpty()){
            throw new UnknownElement("NO SUCH CONNECTION TO: "+peer);
        }
        return connections.get(0);
    }
    public String createStream(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args) {
        return createStreamLogics(peer,type,args,null);
    }
    public String createStreamLogics(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args,String conId) {
        if(conId == null){
            conId = nextId();
        }
        String finalConId = conId;
        try{
            CustomQUICConnection customQUICConnection = getOrThrow(peer);
            QuicStreamChannel quicStreamChannel = QUICLogics.createStream(customQUICConnection.getConnection(),this,metrics, customQUICConnection.isInComing());
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
                                quicStreamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,false));
                            }
                            ((QuicStreamReadHandler) quicStreamChannel.pipeline().get(QuicStreamReadHandler.HANDLER_NAME)).notifyAppDelimitedStreamCreated(quicStreamChannel,type,args, finalConId, false);
                        }else{
                            //future.cause().printStackTrace();
                            quicStreamChannel.close();
                            quicStreamChannel.disconnect();
                            quicStreamChannel.shutdown();
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
            overridenMethods.failedToCreateStream(peer,e.getCause());
            return null;
        }
        return finalConId;
    }
    public void closeLink(String customId){
        try{
            CustomQUICStreamCon nettyQuicStreamId = customStreamIdToStream.get(customId);
            if(nettyQuicStreamId==null){
                logger.debug("UNKNOWN STREAM ID: {}",customId);
            }else{
                nettyQuicStreamId.close();
            }
        }catch (Exception e){
            overridenMethods.failedToCloseStream(customId,e.getCause());
        }
    }

    public void send(String customId, byte[] message, int len, TransmissionType type) {
        CustomQUICStreamCon streamCon = customStreamIdToStream.get(customId);
        if(streamCon==null){
            overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN STREAM ID: "+customId), null, type);
        }else {
            sendMessage(streamCon.streamChannel,message,len,streamCon.customQUICConnection.getRemote(),type);
        }
    }
    public void send(InetSocketAddress peer, byte[] message, int len, TransmissionType type){
        CustomQUICConnection customQUICConnection = getCustomQUICConnection(peer);
        if(customQUICConnection==null){
            QUICConnectingOBJ pendingMessages = connecting.get(peer);
            if( pendingMessages !=null ){
                pendingMessages.msgWithLen.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if (connectIfNotConnected){
                openLogics(peer,type,null);
                connecting.get(peer).msgWithLen.add(Pair.of(message,len));
            }else{
                overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN CONNECTION TO "+peer),peer, type);
            }
        }else{
            sendMessage(customQUICConnection.getDefaultStream().streamChannel,message,len,peer,type);
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
        return addressToQUICCons.containsKey(peer);
    }
    public final String [] getStreams(){
        return customStreamIdToStream.keySet().toArray(new String[customStreamIdToStream.size()]);
    }
    public final InetSocketAddress [] getAddressToQUICCons(){
        return addressToQUICCons.keySet().toArray(new InetSocketAddress[addressToQUICCons.size()]);
    }
    public final int connectedPeers(){
        return addressToQUICCons.size();
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
        CustomQUICConnection connection = getCustomQUICConnection(peer);
        if(connection==null){
            throw new NoSuchElementException("UNKNOWN ELEMENT "+peer);
        }
        return connection.transmissionType;
    }

    @Override
    public TransmissionType getConnectionType(String streamId) {
        try{
            return customStreamIdToStream.get(streamId).type;
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
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause){
        connecting.remove(peer);
        overridenMethods.onOpenConnectionFailed(peer,cause);
    }


}
