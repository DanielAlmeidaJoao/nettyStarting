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
import quicSupport.utils.CustomConnection;
import quicSupport.utils.CustomQUICStream;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.entities.PendingConnectionObj;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Map<InetSocketAddress, PendingConnectionObj> connecting;

    private final Map<String,String> customIdToNettyId;
    private QuicClientExample client;
    private QuicServerExample server;
    private final Properties properties;
    private final boolean withHeartBeat;
    private QuicChannelMetrics metrics;
    private static long heartBeatTimeout;
    private final boolean connectIfNotConnected;
    private final ChannelHandlerMethods overridenMethods;
    private final AtomicInteger idCounter;

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
        connections = TCPStreamUtils.getMapInst(singleThreaded);
        channelIds = TCPStreamUtils.getMapInst(singleThreaded);
        streamHostMapping = TCPStreamUtils.getMapInst(singleThreaded);
        connecting= TCPStreamUtils.getMapInst(singleThreaded);
        customIdToNettyId = TCPStreamUtils.getMapInst(singleThreaded);

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
        idCounter = new AtomicInteger();
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

    public String nextId(){
        return "quicchan"+ TCPStreamUtils.channelIdCounter.getAndIncrement();
    }
    public void streamClosedHandler(QuicStreamChannel channel) {
        String streamId = channel.id().asShortText();
        logger.info("{}. STREAM {} CLOSED",self,streamId);
        InetSocketAddress peer = streamHostMapping.remove(streamId);
        if(peer!=null){
            overridenMethods.onStreamClosedHandler(peer,streamId);
        }
    }

    public void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, Triple<Short,Short,Short> triple, String customId, boolean inConnection) {
        customIdToNettyId.put(customId,channel.id().asShortText());
        InetSocketAddress peer = channelIds.get(channel.parent().id().asShortText());
        if(peer!=null){//THE FIRST STREAM IS DEFAULT. NOT NOTIFIED TO THE CLIENT
            String streamId = channel.id().asShortText();
            streamHostMapping.put(streamId,peer);
            logger.info("{}. STREAM CREATED {}",self,customId);
            connections.get(peer).addStream(new CustomQUICStream(channel,customId,type));
            //overridenMethods.onStreamCreatedHandler(peer,customId,type,triple);
            sendPendingMessages(peer,type,channel.id().asShortText());
            overridenMethods.onConnectionUp(inConnection,peer,type,customId);
        }
    }




    public void onReceivedDelimitedMessage(String streamId, byte[] bytes){
        InetSocketAddress remote = streamHostMapping.get(streamId);
        if(remote==null){return;}
        CustomConnection connection = connections.get(remote);
        if(connection!=null){
            if(withHeartBeat){connection.scheduleSendHeartBeat_KeepAlive();}
            //logger.info("SELF:{} - STREAM_ID:{} REMOTE:{}. RECEIVED {} DATA BYTES.",self,streamId,remote,bytes.length);
            overridenMethods.onChannelReadDelimitedMessage(connection.getStream(streamId).customStreamId,bytes,remote);
        }
    }
    public void onReceivedStream(String streamId, byte [] bytes){
        InetSocketAddress remote = streamHostMapping.get(streamId);
        if(remote==null){return;}
        CustomConnection connection = connections.get(remote);
        if(connection!=null){
            overridenMethods.onChannelReadFlowStream(connection.getStream(streamId).customStreamId,bytes,remote);
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

    private void sendPendingMessages(InetSocketAddress peer, TransmissionType type, String streamId){
        PendingConnectionObj pendingConnectionObj = connecting.remove(peer);
        if(pendingConnectionObj == null){
            return;
        }
        customIdToNettyId.put(pendingConnectionObj.conId,streamId);
        List<Pair<byte[],Integer>> messages = pendingConnectionObj.msgWithLen;
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",getSelf(),messages.size(),peer);
            for (Pair<byte[],Integer> message : messages) {
                send(peer,message.getLeft(),message.getRight(),type);
            }
        }
        if(pendingConnectionObj.connectionsToOpen!=null){
            short p = -1;
            for (Pair<String,TransmissionType> customConId : pendingConnectionObj.connectionsToOpen) {
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
            if(controlData==null){//is OutGoing
                listeningAddress = remotePeer;
            }else{//is InComing
                handShakeMessage = gson.fromJson(new String(controlData),QuicHandShakeMessage.class);
                listeningAddress = handShakeMessage.getAddress();
                type = handShakeMessage.transmissionType;
                inConnection=true;
                connecting.put(listeningAddress,new PendingConnectionObj(nextId(),listeningAddress));
            }
            if(TransmissionType.UNSTRUCTURED_STREAM==type){
                streamChannel.pipeline().replace(QuicMessageEncoder.HANDLER_NAME,QuicUnstructuredStreamEncoder.HANDLER_NAME,new QuicUnstructuredStreamEncoder(metrics));
                streamChannel.pipeline().replace(QuicDelimitedMessageDecoder.HANDLER_NAME,QUICRawStreamDecoder.HANDLER_NAME,new QUICRawStreamDecoder(this,metrics,inConnection));
            }
            CustomConnection current =  new CustomConnection(new CustomQUICStream(streamChannel,connecting.get(listeningAddress).conId,type),
                    listeningAddress,inConnection,withHeartBeat,heartBeatTimeout,type);
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
                                keepOldSilently(current.getDefaultStream(), listeningAddress, old);
                                logger.info("KEPT OLD STREAM {}. IN CONNECTION: {} FROM {}",old.getDefaultStream().streamChannel.id().asShortText(),inConnection,listeningAddress);
                                sendPendingMessages(listeningAddress,type, streamChannel.id().asShortText());
                                return;
                            }
                        }else if(comp>0){
                            //keep the out connection
                            if(inConnection){
                                keepOldSilently(current.getDefaultStream(), listeningAddress, old);
                                logger.info("KEPT OLD STREAM {}. IN CONNECTION: {} FROM {}",streamChannel.id().asShortText(),inConnection,listeningAddress);
                                sendPendingMessages(listeningAddress,type, streamChannel.id().asShortText());
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
            sendPendingMessages(listeningAddress,type,streamChannel.id().asShortText());
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),inConnection);
            }
            overridenMethods.onConnectionUp(inConnection,listeningAddress,type,current.getDefaultStream().customStreamId);
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }

    private void keepOldSilently(CustomQUICStream streamChannel, InetSocketAddress listeningAddress, CustomConnection old) {
        connections.put(listeningAddress, old);
        streamChannel.streamChannel.parent().close();
    }

    private void silentlyCloseCon(CustomQUICStream streamChannel){
        channelIds.remove(streamChannel.streamChannel.parent().id().asShortText());
        streamHostMapping.remove(streamChannel.streamChannel.id().asShortText());
        streamChannel.streamChannel.parent().close();
        customIdToNettyId.remove(streamChannel.customStreamId);
    }

    public  void channelInactive(String channelId){
        try{
            InetSocketAddress host = channelIds.remove(channelId);
            if(host!=null){
                CustomConnection connection = connections.remove(host);
                logger.info("{} CONNECTION TO {} IS DOWN.",self,connection.getRemote());
                connection.close();
                //overridenMethods.onConnectionDown(host,connection.isInComing());
            }
        }catch (Exception e){
            logger.debug(e.getLocalizedMessage());
        }
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
        PendingConnectionObj pendingConnectionObj = connecting.get(peer);
        if(pendingConnectionObj != null){
            pendingConnectionObj.openConnection(id,transmissionType);
            return id;
        }else if(connections.containsKey(peer)){
            Short p = -1;
            return createStreamLogics(peer,transmissionType,Triple.of(p,p,p),id);
        }
        connecting.put(peer,new PendingConnectionObj(id,peer));
        logger.info("{} CONNECTING TO {}",self,peer);
        try {
            client.connect(peer,properties, transmissionType);
        }catch (Exception e){
            e.printStackTrace();
            handleOpenConnectionFailed(peer,e.getCause());
        }
        return id;
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
    public String createStream(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args) {
        return createStreamLogics(peer,type,args,null);
    }
    public String createStreamLogics(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args,String conId) {
        if(conId == null){
            conId = nextId();
        }
        String finalConId = conId;
        try{
            CustomConnection customConnection = getOrThrow(peer);
            QuicStreamChannel quicStreamChannel = QUICLogics.createStream(customConnection.getConnection(),this,metrics,customConnection.isInComing());
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
            String nettyQuicStreamId = customIdToNettyId.remove(customId);
            InetSocketAddress host = streamHostMapping.get(nettyQuicStreamId);
            if(host==null){
                logger.debug("UNKNOWN STREAM ID: {}",customId);
            }else{
                CustomConnection connection = connections.get(host);
                connection.closeStream(customId);
            }
        }catch (Exception e){
            overridenMethods.failedToCloseStream(customId,e.getCause());
        }
    }

    public void send(String streamId, byte[] message, int len, TransmissionType type) {
        try{
            String quicStreamId = customIdToNettyId.get(streamId);
            InetSocketAddress host = streamHostMapping.get(quicStreamId);
            if(host==null){
                overridenMethods.onMessageSent(message,len,new Throwable("UNKNOWN STREAM ID: "+streamId), null, type);
            }else {
                sendMessage(getOrThrow(host).getStream(quicStreamId).streamChannel,message,len,host,type);
            }
        }catch (UnknownElement e){
            logger.debug(e.getMessage());
            overridenMethods.onMessageSent(message,len,e,null,type);
        }
    }
    public void send(InetSocketAddress peer, byte[] message, int len, TransmissionType type){
        CustomConnection connection = connections.get(peer);
        if(connection==null){
            PendingConnectionObj pendingMessages = connecting.get(peer);
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
            sendMessage(connection.getDefaultStream().streamChannel,message,len,peer,type);
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
            streamId = customIdToNettyId.get(streamId);
            InetSocketAddress peer = streamHostMapping.get(streamId);
            CustomConnection connection = connections.get(peer);
            System.out.println("CALLELDE CALLED");
            return connection.getStream(streamId).type;
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
