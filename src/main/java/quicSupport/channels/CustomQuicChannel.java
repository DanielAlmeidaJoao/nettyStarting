package quicSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
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
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.customConnections.CustomQUICConnection;
import quicSupport.utils.customConnections.CustomQUICStreamCon;
import quicSupport.utils.entities.QUICConnectingOBJ;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import quicSupport.utils.metrics.QuicConnectionMetrics;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.IOException;
import java.io.InputStream;
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
            overridenMethods.onStreamClosedHandler(streamCon.customQUICConnection.getRemote(),streamCon.customStreamId,streamCon.inConnection);
        }
    }
    public void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, String customId, boolean inConnection) {
        logger.info("{}. STREAM CREATED {}",self,customId);
        System.out.println("CREATED "+channel.id().asShortText());
        CustomQUICStreamCon parent = nettyIdToStream.get(channel.parent().id().asShortText());
        CustomQUICStreamCon con = new CustomQUICStreamCon(channel,customId,type,parent.customQUICConnection,inConnection);
        con.customQUICConnection.addStream(con);
        customStreamIdToStream.put(customId,con);
        nettyIdToStream.put(channel.id().asShortText(),con);
        sendPendingMessages(con.customQUICConnection.getRemote(), type,channel.id().asShortText());
        overridenMethods.onConnectionUp(inConnection,con.customQUICConnection.getRemote(),type,customId);
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
                streamChannel.pipeline().replace(QuicStructuredMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,inConnection));
            }
            CustomQUICStreamCon quicStreamChannel = new CustomQUICStreamCon(streamChannel,customConId, type,null, inConnection);
            CustomQUICConnection current =  new CustomQUICConnection(quicStreamChannel, listeningAddress,inConnection,withHeartBeat,heartBeatTimeout,type);
            quicStreamChannel.customQUICConnection = current;
            synchronized (this){
                List<CustomQUICConnection> connections = addressToQUICCons.get(listeningAddress);
                if(connections == null){
                    connections = new LinkedList<>();
                    addressToQUICCons.put(listeningAddress,connections);
                }
                connections.add(current);
            }
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
            customQUICConnection.getConnection()
                    .createStream(QuicStreamType.BIDIRECTIONAL, new ServerChannelInitializer(this,metrics,false))
                    .addListener(future -> {
                        if(future.isSuccess() ){
                            if(metrics!=null){
                                QuicConnectionMetrics q = metrics.getConnectionMetrics(customQUICConnection.getConnection().remoteAddress());
                                q.setCreatedStreamCount(q.getCreatedStreamCount()+1);
                            }
                            QuicStreamChannel streamChannel = (QuicStreamChannel) future.getNow();
                            streamChannel.writeAndFlush(QUICLogics.bufToWrite(type.ordinal(),STREAM_CREATED))
                                    .addListener(future1 -> {
                                        if(future.isSuccess()){
                                            if(TransmissionType.UNSTRUCTURED_STREAM == type){
                                                streamChannel.pipeline().replace(QuicStructuredMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                                                streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,false));
                                            }
                                            ((QuicStreamReadHandler) streamChannel.pipeline().get(QuicStreamReadHandler.HANDLER_NAME)).notifyAppDelimitedStreamCreated(streamChannel,type, finalConId, false);
                                        }
                                    });
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
    public boolean containConnection(String customId){
        return customStreamIdToStream.containsKey(customId);
    }
    public boolean containConnection(InetSocketAddress address){
        CustomQUICConnection customQUICConnection = getCustomQUICConnection(address);
        if(customQUICConnection==null){
            return false;
        }else{
            return customQUICConnection.getDefaultStream()!=null;
        }
    }


    public boolean send(String customId, byte[] message, int len, TransmissionType type) {
        boolean sent = false;
        CustomQUICStreamCon streamCon = customStreamIdToStream.get(customId);
        if(streamCon==null){
            overridenMethods.onMessageSent(message,null, len,new Throwable("UNKNOWN STREAM ID: "+customId), null, type);
        }else {
            sent = true;
            sendMessage(streamCon,message,len,streamCon.customQUICConnection.getRemote(),type);
        }
        return sent;
    }
    public boolean send(InetSocketAddress peer, byte[] message, int len, TransmissionType type){
        boolean sent = false;
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
                overridenMethods.onMessageSent(message,null,len,new Throwable("UNKNOWN CONNECTION TO "+peer),peer, type);
            }
        }else{
            sendMessage(customQUICConnection.getDefaultStream(),message,len,peer,type);
            sent = true;
        }
        return sent;
    }
    protected void sendMessage(CustomQUICStreamCon streamChannel, byte[] message, int len, InetSocketAddress peer, TransmissionType type){
        if(streamChannel.type!=type){
            throw new RuntimeException("WRONG MESSAGE. EXPECTED TYPE: "+streamChannel.type+" VS RECEIVED TYPE: "+type);
        }
        ChannelFuture c;
        if(TransmissionType.UNSTRUCTURED_STREAM==type){
            c = streamChannel.streamChannel.writeAndFlush(Unpooled.buffer(len).writeBytes(message,0,len));
        }else{
            c = streamChannel.streamChannel.writeAndFlush(QUICLogics.bufToWrite(len,message,APP_DATA));
        }
        c.addListener(future -> {
            if(future.isSuccess()){
                overridenMethods.onMessageSent(message,null, len,null,peer,type);
            }else{
                overridenMethods.onMessageSent(message, null, len,future.cause(),peer,type);
            }
        });
    }
    public boolean sendInputStream(InputStream inputStream, int len, InetSocketAddress peer,String conId)  {
        try {
            CustomQUICStreamCon streamChannel = customStreamIdToStream.get(conId);
            CustomQUICConnection connection = getCustomQUICConnection(peer);
            if(streamChannel == null && connection == null ){
                overridenMethods.onMessageSent(null,inputStream,len,new Throwable("FAILED TO SEND INPUTSTREAM. UNKNOWN PEER AND CONID: "+peer+" - "+conId),peer,TransmissionType.UNSTRUCTURED_STREAM);
                return false;
            }else if(streamChannel == null ){
                streamChannel = connection.getDefaultStream();
            }
            if(streamChannel.type!=TransmissionType.UNSTRUCTURED_STREAM){
                Throwable t = new Throwable("INPUTSTREAM CAN ONLY BE SENT WITH UNSTRUCTURED STREAM TRANSMISSION TYPE");
                peer = streamChannel.customQUICConnection.getRemote();
                overridenMethods.onMessageSent(null,inputStream,len,t,peer,TransmissionType.UNSTRUCTURED_STREAM);
                return false;
            }
            final ByteBuf buf = Unpooled.buffer(len);
            buf.writeBytes(inputStream,len);
            ChannelFuture c = streamChannel.streamChannel.writeAndFlush(buf);
            InetSocketAddress finalPeer = peer;
            c.addListener(future -> {
                if(!future.isSuccess()){
                    future.cause().printStackTrace();
                    overridenMethods.onMessageSent(new byte[0], inputStream, len,future.cause(), finalPeer,TransmissionType.UNSTRUCTURED_STREAM);
                }
            });
            return true;
        }catch (Exception e){
            e.printStackTrace();
            overridenMethods.onMessageSent(null,inputStream,0,e.getCause(),peer,TransmissionType.UNSTRUCTURED_STREAM);
        }
        return false;
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
            return null;
        }
        return connection.transmissionType;
    }

    @Override
    public TransmissionType getConnectionType(String streamId) {
        try{
            return customStreamIdToStream.get(streamId).type;
        }catch (Exception e){
            return null;
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
