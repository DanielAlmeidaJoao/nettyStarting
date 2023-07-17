package tcpSupport.tcpChannelAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.NettyChannelInterface;
import quicSupport.channels.SendStreamInterface;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.connectionSetups.StreamInConnection;
import tcpSupport.tcpChannelAPI.connectionSetups.StreamOutConnection;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpChannelAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpChannelAPI.metrics.TCPStreamMetrics;
import tcpSupport.tcpChannelAPI.utils.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import static quicSupport.utils.enums.TransmissionType.STRUCTURED_MESSAGE;


public class NettyTCPChannel implements StreamingNettyConsumer, NettyChannelInterface, SendStreamInterface {
    private static final Logger logger = LogManager.getLogger(NettyTCPChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";
    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private final Map<String, CustomTCPConnection> nettyIdToConnection;
    private final Map<String, TCPConnectingObject> nettyIdTOConnectingOBJ;
    private final Map<InetSocketAddress, ConcurrentLinkedQueue<CustomTCPConnection>> addressToConnections;
    private final Map<String,CustomTCPConnection> customIdToConnection;
    private final StreamInConnection server;
    private final StreamOutConnection client;
    private final boolean metricsOn;
    private final TCPStreamMetrics tcpStreamMetrics;
    private final boolean connectIfNotConnected;
    private final ChannelHandlerMethods channelHandlerMethods;
    private SendStreamContinuoslyLogics streamContinuoslyLogics;
    private Properties properties;

    public NettyTCPChannel(Properties properties, boolean singleThreaded, ChannelHandlerMethods chm, NetworkRole networkRole)throws IOException{
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        metricsOn = properties.containsKey("metrics");
        if(metricsOn){
            tcpStreamMetrics = new TCPStreamMetrics(self,singleThreaded);
        }else{
            tcpStreamMetrics = null;
        }
        nettyIdToConnection = TCPStreamUtils.getMapInst(singleThreaded);
        addressToConnections = TCPStreamUtils.getMapInst(singleThreaded);
        nettyIdTOConnectingOBJ = TCPStreamUtils.getMapInst(singleThreaded);
        customIdToConnection = TCPStreamUtils.getMapInst(singleThreaded);
        if(NetworkRole.CHANNEL==networkRole||NetworkRole.SERVER==networkRole){
            server = new StreamInConnection(addr.getHostName(),port);
            try{
                server.startListening(false,tcpStreamMetrics,this);
            }catch (Exception e){
                throw new IOException(e);
            }
        }else{
            server = null;
        }
        if(NetworkRole.CHANNEL==networkRole||NetworkRole.CLIENT==networkRole){
            if(NetworkRole.CLIENT==networkRole){
                properties.remove(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP);
            }
            client = new StreamOutConnection(self);
        }else{
            client=null;
        }

        connectIfNotConnected = properties.getProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP)!=null;

        this.channelHandlerMethods = chm;
        streamContinuoslyLogics = null;
        this.properties = properties;
    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public  void onChannelInactive(String channelId){
        channelInactiveLogics(channelId);
    }
    private void channelInactiveLogics(String channelId){
        nettyIdTOConnectingOBJ.remove(channelId);
        CustomTCPConnection connection = nettyIdToConnection.remove(channelId);
        if(connection == null){
            return;
        }
        customIdToConnection.remove(connection.conId);
        ConcurrentLinkedQueue<CustomTCPConnection> connections = addressToConnections.get(connection.host);
        if(connections!=null && connections.remove(connection) && connections.isEmpty()){
            addressToConnections.remove(connection.host);
        }
        if(metricsOn){
            tcpStreamMetrics.onConnectionClosed(connection.channel.remoteAddress());
        }
        channelHandlerMethods.onStreamClosedHandler(connection.host,connection.conId,connection.inConnection);
    }

    public void onChannelMessageRead(String channelId, byte[] bytes){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.debug("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            channelHandlerMethods.onChannelReadDelimitedMessage(connection.conId,bytes,connection.host);
        }
    }
    public void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.debug("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            channelHandlerMethods.onChannelReadFlowStream(connection.conId,babelOutputStream,connection.host,connection.inputStream);
        }
    }
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type){
        try {
            boolean inConnection;
            InetSocketAddress listeningAddress;
            String conId;
            if(handShakeMessage==null){//out connection
                listeningAddress = (InetSocketAddress) channel.remoteAddress();
                inConnection = false;
                conId = channel.attr(AttributeKey.valueOf(TCPStreamUtils.CUSTOM_ID_KEY)).toString();
            }else {//in connection
                listeningAddress = handShakeMessage.getAddress();
                inConnection = true;
                conId = nextId();
            }

            BabelInputStream babelInputStream = BabelInputStream.toBabelStream(conId,this,type);
            CustomTCPConnection connection = new CustomTCPConnection(channel,type,listeningAddress,conId,inConnection,babelInputStream);
            nettyIdToConnection.put(channel.id().asShortText(),connection);
            customIdToConnection.put(conId,connection);
            synchronized (this){
                addressToConnections.computeIfAbsent(listeningAddress, k -> new ConcurrentLinkedQueue<>()).add(connection);;
            }

            if(metricsOn){
                tcpStreamMetrics.updateConnectionMetrics(channel.remoteAddress(),listeningAddress,inConnection);
            }
            sendPendingMessages(connection,type);
            channelHandlerMethods.onConnectionUp(connection.inConnection,connection.host,type,conId, babelInputStream);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }
    private void sendPendingMessages(CustomTCPConnection customTCPConnection, TransmissionType type){
        TCPConnectingObject connectingObject = nettyIdTOConnectingOBJ.remove(customTCPConnection.conId);
        if(connectingObject!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self
                    ,connectingObject.pendingMessages.size(),customTCPConnection.host);
            for (Pair<byte[],Integer> message : connectingObject.pendingMessages) {
                send(customTCPConnection.host,message.getLeft(),message.getRight());
            }
        }
    }
    public void onConnectionFailed(String channelId, Throwable cause, TransmissionType type){
        CustomTCPConnection customTCPConnection = nettyIdToConnection.get(channelId);
        //channelInactiveLogics(channelId);
        if(customTCPConnection != null){
            handleOpenConnectionFailed(customTCPConnection.host,cause,type,customTCPConnection.conId);
        }
    }
    public String nextId(){
        return "tcpChan"+ TCPStreamUtils.channelIdCounter.getAndIncrement();
    }
    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/
    public String open(InetSocketAddress peer, TransmissionType type) {
        return openConnectionLogics(peer,type,null);
    }
    private String isConnecting(InetSocketAddress peer){
        for (TCPConnectingObject value : nettyIdTOConnectingOBJ.values()) {
            if(value.dest.equals(peer)){
                return value.customId;
            }
        }
        return null;
    }
    public String openConnectionLogics(InetSocketAddress peer, TransmissionType type, String conId) {
        if(!connectIfNotConnected){
            String connectionId = isConnecting(peer);
            if(connectionId!=null) return connectionId;
            try{
                return addressToConnections.get(peer).peek().conId;
            }catch (Exception e){};
        }
        if(conId == null ){
            conId = nextId();
        }
        nettyIdTOConnectingOBJ.put(conId,new TCPConnectingObject(conId,peer,new LinkedList<>()));
        logger.debug("{} CONNECTING TO {}. CONNECTION ID: ",self,peer,conId);
        try {
            client.connect(peer,tcpStreamMetrics,this,type,conId);
            return conId;
        }catch (Exception e){
            e.printStackTrace();
            handleOpenConnectionFailed(peer,e.getCause(), type,conId);
        }
        return null;
    }
    public void closeConnection(InetSocketAddress peer) {
        logger.info("CLOSING ALL CONNECTIONS TO {}", peer);
        ConcurrentLinkedQueue<CustomTCPConnection> connections = addressToConnections.remove(peer);
        if(connections!=null){
            for (CustomTCPConnection connection : connections) {
                connection.close();
            }
        }
    }

    @Override
    public void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler) {
        new Exception("TO DO").printStackTrace();
    }

    @Override
    public void closeLink(String connectionId) {
        CustomTCPConnection connection = customIdToConnection.remove(connectionId);
        if(connection!=null){
            connection.close();
        }
    }
    public void closeServerSocket(){
        server.closeServerSocket();
    }
    public void send(String customConId, byte[] message, int len){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onMessageSent(message, null, len,new Throwable("Unknown Connection ID : "+customConId),null, STRUCTURED_MESSAGE);
        }else{
            send(message,len,connection);
        }
    }
    private void calcMetricsOnSend(Future future, Channel channel, int length){
        if(future.isSuccess()){
            if(metricsOn){
                TCPStreamConnectionMetrics metrics1 = tcpStreamMetrics.getConnectionMetrics(channel.remoteAddress());
                metrics1.setSentAppBytes(metrics1.getSentAppBytes()+length);
                metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
            }
        }
    }
    private void send(byte[] message, int len, CustomTCPConnection connection){
        if(connection.type== STRUCTURED_MESSAGE){
            ByteBuf byteBuf = Unpooled.directBuffer(len+4).writeInt(len).writeBytes(message,0,len);
            final int sentBytes = byteBuf.readableBytes();
            ChannelFuture f = connection.channel.writeAndFlush(byteBuf);
            f.addListener(future -> {
                calcMetricsOnSend(future,connection.channel,sentBytes);
                channelHandlerMethods.onMessageSent(message,null,len,future.cause(),connection.host,STRUCTURED_MESSAGE);
            });
        }else{
            channelHandlerMethods.onMessageSent(message, null,len,new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND EXPECTING "+ STRUCTURED_MESSAGE+" DATA TYPE"),connection.host, STRUCTURED_MESSAGE);
        }
    }

    public void sendStream(String customConId,ByteBuf byteBuf,boolean flush){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onMessageSent(new byte[0], null,byteBuf.readableBytes(),new Throwable("Unknown Connection ID : "+customConId),null,TransmissionType.UNSTRUCTURED_STREAM);
        }else{
            final int toSend = byteBuf.readableBytes();
            ChannelFuture f;
            if(flush){
                f = connection.channel.writeAndFlush(byteBuf);
            }else{
                f = connection.channel.write(byteBuf);
            }
            f.addListener(future -> {
                calcMetricsOnSend(future,connection.channel,toSend);
                channelHandlerMethods.onMessageSent(new byte[0],null,toSend,future.cause(),connection.host,TransmissionType.UNSTRUCTURED_STREAM);
            });
        }
    }
    @Override
    public boolean flushStream(String conId) {
        CustomTCPConnection connection = customIdToConnection.get(conId);
        if(connection!=null){
            connection.channel.flush();
            return true;
        }
        return false;
    }
    public void sendInputStream( String conId, InputStream inputStream, long len)  {
        try {
            CustomTCPConnection idConnection = customIdToConnection.get(conId);
            if(idConnection == null){
                channelHandlerMethods.onMessageSent(null,inputStream,inputStream.available(), new Throwable("FAILED TO SEND INPUTSTREAM. UNKNOWN CONID: "+conId),null,null);
                return;
            }
            if(idConnection.type!=TransmissionType.UNSTRUCTURED_STREAM){
                Throwable t = new Throwable("INPUTSTREAM CAN ONLY BE SENT WITH UNSTRUCTURED STREAM TRANSMISSION TYPE. CON ID:"+conId);
                channelHandlerMethods.onMessageSent(null,inputStream,inputStream.available(),t,idConnection.host, STRUCTURED_MESSAGE);
                return;
            }
            if(len<=0){
                if(streamContinuoslyLogics==null)streamContinuoslyLogics = new SendStreamContinuoslyLogics(this,properties.getProperty(TCPStreamUtils.READ_STREAM_PERIOD_KEY));
                streamContinuoslyLogics.addToStreams(inputStream,idConnection.conId,idConnection.channel.eventLoop());
                return;
            }
            ChannelFuture c;
            if(inputStream instanceof FileInputStream){
                FileInputStream in = (FileInputStream) inputStream;
                FileRegion region = new DefaultFileRegion(in.getChannel(), 0,len);
                c = idConnection.channel.writeAndFlush(region);
            }else{
                if(idConnection.channel.pipeline().get("ChunkedWriteHandler")==null){
                    idConnection.channel.pipeline().addLast("ChunkedWriteHandler",new ChunkedWriteHandler());
                }
                c = idConnection.channel.writeAndFlush(new ChunkedStream(inputStream));
            }

            CustomTCPConnection finalIdConnection = idConnection;
            c.addListener(future -> {
                channelHandlerMethods.onMessageSent(null,inputStream, (int) len, future.cause(),finalIdConnection.host,TransmissionType.UNSTRUCTURED_STREAM);
            });
        }catch (Exception e){
            channelHandlerMethods.onMessageSent(null,inputStream,(int) len,e.getCause(),null,TransmissionType.UNSTRUCTURED_STREAM);
        }
    }
    private TCPConnectingObject connectingObject(InetSocketAddress peer){
        for (TCPConnectingObject value : nettyIdTOConnectingOBJ.values()) {
            if(value.dest.equals(peer)){
                return value;
            }
        }
        return null;
    }
    public void send(InetSocketAddress peer, byte[] message, int len){
        var connections = addressToConnections.get(peer);
        CustomTCPConnection connection=null;
        if(connections != null && !connections.isEmpty()){
            connection=connections.peek();
        }
        if(connections == null || connection == null){
            TCPConnectingObject pendingMessages  = connectingObject(peer);
            if( pendingMessages != null ){
                pendingMessages.pendingMessages.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                String conId = openConnectionLogics(peer,STRUCTURED_MESSAGE,null);
                nettyIdTOConnectingOBJ.get(conId).pendingMessages.add(Pair.of(message,len));
            }else{
                channelHandlerMethods.onMessageSent(message,null,len,new Throwable("Unknown Peer : "+peer),peer, STRUCTURED_MESSAGE);
            }
        }else {
            send(message,len,connection);
        }
    }

    @Override
    public boolean enabledMetrics() {
        return metricsOn;
    }

    @Override
    public boolean isConnected(InetSocketAddress peer) {
        return addressToConnections.containsKey(peer);
    }

    @Override
    public String[] getStreams() {
        return customIdToConnection.keySet().toArray(new String[customIdToConnection.size()]);
    }

    @Override
    public InetSocketAddress[] getAddressToQUICCons() {
        return addressToConnections.keySet().toArray(new InetSocketAddress[addressToConnections.size()]);
    }

    @Override
    public int connectedPeers() {
        return addressToConnections.size();
    }

    @Override
    public void shutDown() {
        if(server!=null){
            server.closeServerSocket();
        }
        if(client!=null){
            client.shutDown();
        }
    }

    @Override
    public TransmissionType getConnectionType(InetSocketAddress peer){
        ConcurrentLinkedQueue<CustomTCPConnection> cons = addressToConnections.get(peer);
        if(cons==null || cons.isEmpty()){
            return null;
        }else{
            return cons.peek().type;
        }
    }

    @Override
    public TransmissionType getConnectionType(String streamId){
        CustomTCPConnection connection = customIdToConnection.get(streamId);
        if(connection==null){
            return null;
        }
        return connection.type;
    }

    /******************************************* USER EVENTS END ****************************************************/

    public void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType type, String conId){
        channelHandlerMethods.onOpenConnectionFailed(peer,cause,type,conId);
    }

    public void readMetrics(ReadMetricsHandler handler) throws MetricsDisabledException {
        if(metricsOn){
            handler.readMetrics(tcpStreamMetrics.currentMetrics(),tcpStreamMetrics.oldMetrics());
        }else {
            throw new MetricsDisabledException("METRICS WAS NOT ENABLED!");
        }
    }
    @Override
    public void readMetrics(QuicReadMetricsHandler handler) {
        new Exception("TO DO").printStackTrace();
    }
}
