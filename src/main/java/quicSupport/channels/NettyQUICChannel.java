package quicSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
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
import tcpSupport.tcpStreamingAPI.utils.BabelInputStream;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;
import tcpSupport.tcpStreamingAPI.utils.SendStreamContinuoslyLogics;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static quicSupport.utils.QUICLogics.*;
import static quicSupport.utils.enums.TransmissionType.STRUCTURED_MESSAGE;

public class NettyQUICChannel implements CustomQuicChannelConsumer, NettyChannelInterface, SendStreamInterface {
    private static final Logger logger = LogManager.getLogger(NettyQUICChannel.class);

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
    private SendStreamContinuoslyLogics streamContinuoslyLogics;

    public NettyQUICChannel(Properties properties, boolean singleThreaded, NetworkRole networkRole, ChannelHandlerMethods mom)throws IOException {
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
        streamContinuoslyLogics = null;
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
            overridenMethods.onStreamErrorHandler(streamCon.customParentConnection.getRemote(), throwable,streamId);
        }
    }

    public String nextId(){
        return "quicchan"+ TCPStreamUtils.channelIdCounter.getAndIncrement();
    }

    private CustomQUICConnection getCustomQUICConnection(InetSocketAddress inetSocketAddress){
        if(inetSocketAddress==null) return null;
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
            streamCon.customParentConnection.closeStream(streamId);
            overridenMethods.onStreamClosedHandler(streamCon.customParentConnection.getRemote(),streamCon.customStreamId,streamCon.inConnection);
        }
    }

    public void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type, String customId, boolean inConnection) {
        logger.info("{}. STREAM CREATED {}",self,customId);
        CustomQUICStreamCon firstStreamOfThisCon = nettyIdToStream.get(channel.parent().id().asShortText());
        BabelInputStream babelInputStream = BabelInputStream.toBabelStream(customId,this,type);
        CustomQUICStreamCon con = new CustomQUICStreamCon(channel,customId,type,firstStreamOfThisCon.customParentConnection,inConnection, babelInputStream);
        con.customParentConnection.addStream(con);
        customStreamIdToStream.put(customId,con);
        nettyIdToStream.put(channel.id().asShortText(),con);
        sendPendingMessages(con.customParentConnection.getRemote(), type);
        overridenMethods.onConnectionUp(inConnection,con.customParentConnection.getRemote(),type,customId, babelInputStream);
    }




    public void onReceivedDelimitedMessage(String streamId, byte[] bytes){
        CustomQUICStreamCon streamCon = nettyIdToStream.get(streamId);
        if(streamCon!=null){
            if(withHeartBeat){streamCon.customParentConnection.scheduleSendHeartBeat_KeepAlive();}
            //logger.info("SELF:{} - STREAM_ID:{} REMOTE:{}. RECEIVED {} DATA BYTES.",self,streamId,remote,bytes.length);
            overridenMethods.onChannelReadDelimitedMessage(streamCon.customStreamId,bytes,streamCon.customParentConnection.getRemote());
        }
    }
    public void onReceivedStream(String streamId, BabelOutputStream babelOutputStream){
        CustomQUICStreamCon streamCon = nettyIdToStream.get(streamId);
        if(streamCon != null){
            overridenMethods.onChannelReadFlowStream(streamCon.customStreamId, babelOutputStream,streamCon.customParentConnection.getRemote(),streamCon.inputStream);
        }
    }

    public void onKeepAliveMessage(String parentId){
        CustomQUICStreamCon streamCon = nettyIdToStream.get(parentId);
        if(streamCon!=null){
            InetSocketAddress remote = streamCon.customParentConnection.getRemote();
            logger.info("SELF:{} -- HEART BEAT RECEIVED -- {}",self,remote);
            if(withHeartBeat){
                streamCon.customParentConnection.scheduleSendHeartBeat_KeepAlive();
            }
        }
    }

    private void sendPendingMessages(InetSocketAddress peer, TransmissionType type){
        QUICConnectingOBJ QUICConnectingOBJ = connecting.remove(peer);
        if(QUICConnectingOBJ == null){
            return;
        }
        List<Pair<byte[],Integer>> messages = QUICConnectingOBJ.msgWithLen;
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",getSelf(),messages.size(),peer);
            for (Pair<byte[],Integer> message : messages) {
                send(peer,message.getLeft(),message.getRight());
            }
        }
        if(QUICConnectingOBJ.connectionsToOpen!=null){
            for (Pair<String,TransmissionType> customConId : QUICConnectingOBJ.connectionsToOpen) {
                createStreamLogics(peer,customConId.getRight(),customConId.getLeft());
            }
        }
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, QuicHandShakeMessage handShakeMessage, InetSocketAddress remotePeer, TransmissionType type){
        boolean inConnection = false;
        try {
            InetSocketAddress listeningAddress;
            String customConId;
            if(handShakeMessage==null){//is OutGoing
                listeningAddress = remotePeer;
                customConId = streamChannel.parent().attr(AttributeKey.valueOf(TCPStreamUtils.CUSTOM_ID_KEY)).toString();
            }else{//is InComing
                listeningAddress = handShakeMessage.getAddress();
                type = handShakeMessage.transmissionType;
                inConnection=true;
                customConId = nextId();
            }

            CustomQUICConnection parentConnection;
            BabelInputStream babelInputStream = BabelInputStream.toBabelStream(customConId,this,type);
            CustomQUICStreamCon quicStreamChannel = new CustomQUICStreamCon(streamChannel,customConId,type,null,inConnection,babelInputStream);
                CustomQUICStreamCon firstStreamOfThisCon = nettyIdToStream.get(streamChannel.parent().id().asShortText());
                if(firstStreamOfThisCon==null){
                    parentConnection = new CustomQUICConnection(quicStreamChannel,listeningAddress,inConnection,withHeartBeat,heartBeatTimeout,type);
                    nettyIdToStream.put(streamChannel.parent().id().asShortText(),quicStreamChannel);
                    synchronized (addressToQUICCons){
                        addressToQUICCons.computeIfAbsent(listeningAddress, k -> new LinkedList<>()).add(parentConnection);
                    }
                }else{
                    parentConnection = firstStreamOfThisCon.customParentConnection;
                }
                quicStreamChannel.customParentConnection = parentConnection;


            nettyIdToStream.put(streamChannel.id().asShortText(),quicStreamChannel);
            customStreamIdToStream.put(customConId,quicStreamChannel);
            sendPendingMessages(listeningAddress,type);
            if(enableMetrics){
                metrics.updateConnectionMetrics(streamChannel.parent().remoteAddress(),listeningAddress,streamChannel.parent().collectStats().get(),inConnection);
            }
            overridenMethods.onConnectionUp(inConnection,listeningAddress,type,customConId, babelInputStream);
        }catch (Exception e){
            e.printStackTrace();
            streamChannel.disconnect();
        }
    }

    public  void channelInactive(String parentId){
        CustomQUICConnection customQUICConnection = nettyIdToStream.remove(parentId).customParentConnection;
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
            return createStreamLogics(peer,transmissionType,id);
        }
        connecting.put(peer,new QUICConnectingOBJ(id,peer));
        logger.info("{} CONNECTING TO {}",self,peer);
        try {
            client.connect(peer,properties, transmissionType,id);
        }catch (Exception e){
            e.printStackTrace();
            handleOpenConnectionFailed(peer,e.getCause(),transmissionType, id);
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

    public String createStreamLogics(InetSocketAddress peer, TransmissionType type,String conId) {
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
                                                streamChannel.pipeline().remove(QuicStructuredMessageEncoder.HANDLER_NAME);
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


    public void send(String customId, byte[] message, int len) {
        CustomQUICStreamCon streamCon = customStreamIdToStream.get(customId);
        if(streamCon==null){
            overridenMethods.onMessageSent(message,null, len,new Throwable("UNKNOWN STREAM ID: "+customId), null,STRUCTURED_MESSAGE);
        }else {
            sendMessage(streamCon,message,len,streamCon.customParentConnection.getRemote());
        }
    }
    public void send(InetSocketAddress peer, byte[] message, int len){
        CustomQUICConnection customQUICConnection = getCustomQUICConnection(peer);
        if(customQUICConnection==null){
            QUICConnectingOBJ pendingMessages = connecting.get(peer);
            if( pendingMessages !=null ){
                pendingMessages.msgWithLen.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if (connectIfNotConnected){
                openLogics(peer,STRUCTURED_MESSAGE,null);
                connecting.get(peer).msgWithLen.add(Pair.of(message,len));
            }else{
                overridenMethods.onMessageSent(message,null,len,new Throwable("UNKNOWN CONNECTION TO "+peer),peer,STRUCTURED_MESSAGE);
            }
        }else{
            sendMessage(customQUICConnection.getDefaultStream(),message,len,peer);
        }
    }
    protected void sendMessage(CustomQUICStreamCon streamChannel, byte[] message, int len, InetSocketAddress peer){
        if(streamChannel.type!=STRUCTURED_MESSAGE){
            throw new RuntimeException("WRONG MESSAGE. EXPECTED TYPE: "+STRUCTURED_MESSAGE+" VS RECEIVED TYPE: "+streamChannel.type);
        }
        ChannelFuture c = streamChannel.streamChannel.writeAndFlush(QUICLogics.bufToWrite(len,message,APP_DATA));
        c.addListener(future -> {
            if(future.isSuccess()){
                overridenMethods.onMessageSent(message,null, len,null,peer,STRUCTURED_MESSAGE);
            }else{
                overridenMethods.onMessageSent(message, null, len,future.cause(),peer,STRUCTURED_MESSAGE);
            }
        });
    }
    private void calcMetricsOnSend(Future future, Channel channel, int length){
        if(future.isSuccess()){
            if(enableMetrics){
                QuicConnectionMetrics q = metrics.getConnectionMetrics(channel.remoteAddress());
                q.setSentAppMessages(q.getSentAppMessages()+1);
                q.setSentAppBytes(q.getSentAppBytes()+length);
            }
        }
    }
    @Override
    public void sendStream(String customConId ,ByteBuf byteBuf,boolean flush){
        CustomQUICStreamCon connection = customStreamIdToStream.get(customConId);
        if(connection == null ){
            overridenMethods.onMessageSent(new byte[0], null,byteBuf.readableBytes(),new Throwable("Unknown Connection ID : "+customConId),null,TransmissionType.UNSTRUCTURED_STREAM);
        }else{
            final int toSend = byteBuf.readableBytes();
            ChannelFuture f;
            if(flush){
                f = connection.streamChannel.writeAndFlush(byteBuf);
            }else{
                f = connection.streamChannel.write(byteBuf);
            }
            f.addListener(future -> {
                calcMetricsOnSend(future,connection.customParentConnection.getConnection(),toSend);
                overridenMethods.onMessageSent(new byte[0],null,toSend,future.cause(),connection.customParentConnection.getRemote(),TransmissionType.UNSTRUCTURED_STREAM);
            });
        }
    }
    public void sendInputStream(String conId, InputStream inputStream, long len)  {
        try {
            CustomQUICStreamCon streamChannel = customStreamIdToStream.get(conId);
            if(streamChannel==null){
                overridenMethods.onMessageSent(null,inputStream,inputStream.available(),new Throwable("FAILED TO SEND INPUTSTREAM. UNKNOWN PEER AND CONID: "+conId),null,TransmissionType.UNSTRUCTURED_STREAM);
                return;
            }
            InetSocketAddress peer = streamChannel.customParentConnection.getRemote();
            if(streamChannel.type!=TransmissionType.UNSTRUCTURED_STREAM){
                Throwable t = new Throwable("INPUTSTREAM CAN ONLY BE SENT WITH UNSTRUCTURED STREAM TRANSMISSION TYPE");
                overridenMethods.onMessageSent(null,inputStream,inputStream.available(),t,peer,TransmissionType.UNSTRUCTURED_STREAM);
                return;
            }

            if(len<=0){
                if(streamContinuoslyLogics==null)streamContinuoslyLogics = new SendStreamContinuoslyLogics(this,properties.getProperty(TCPStreamUtils.READ_STREAM_PERIOD_KEY));
                streamContinuoslyLogics.addToStreams(inputStream,streamChannel.customStreamId,streamChannel.streamChannel.parent().eventLoop());
                return;
            }
            if(streamChannel.streamChannel.pipeline().get("ChunkedWriteHandler")==null){
                streamChannel.streamChannel.pipeline().addLast("ChunkedWriteHandler",new ChunkedWriteHandler());
            }
            ChannelFuture c = streamChannel.streamChannel.writeAndFlush(new ChunkedStream(inputStream));
            InetSocketAddress finalPeer = peer;
            c.addListener(future -> {
                if(!future.isSuccess()){
                    future.cause().printStackTrace();
                    overridenMethods.onMessageSent(new byte[0], inputStream,inputStream.available(),future.cause(), finalPeer,TransmissionType.UNSTRUCTURED_STREAM);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            overridenMethods.onMessageSent(null,inputStream,0,e.getCause(),null,TransmissionType.UNSTRUCTURED_STREAM);
        }
    }

    @Override
    public boolean flushStream(String conId) {
        CustomQUICStreamCon streamChannel = customStreamIdToStream.get(conId);
        if(streamChannel!=null){
            streamChannel.streamChannel.flush();
            return true;
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
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String id){
        connecting.remove(peer);
        overridenMethods.onOpenConnectionFailed(peer,cause,transmissionType,id);
    }




}
