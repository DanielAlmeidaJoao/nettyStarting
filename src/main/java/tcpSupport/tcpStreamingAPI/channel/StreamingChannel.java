package tcpSupport.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.NettyChannelInterface;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.streamUtils.BabelInBytesWrapper;
import tcpSupport.tcpStreamingAPI.connectionSetups.StreamInConnection;
import tcpSupport.tcpStreamingAPI.connectionSetups.StreamOutConnection;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
import tcpSupport.tcpStreamingAPI.utils.MetricsDisabledException;
import tcpSupport.tcpStreamingAPI.utils.SendStreamContinuoslyLogics;
import tcpSupport.tcpStreamingAPI.utils.TCPConnectingObject;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class StreamingChannel implements StreamingNettyConsumer, NettyChannelInterface {
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private final Map<String, CustomTCPConnection> nettyIdToConnection;
    private final Map<String, TCPConnectingObject> nettyIdTOConnectingOBJ;
    private final Map<InetSocketAddress,List<CustomTCPConnection>> addressToConnections;
    private final Map<String,CustomTCPConnection> customIdToConnection;

    private final StreamInConnection server;
    private final StreamOutConnection client;
    private final boolean metricsOn;
    private final TCPStreamMetrics tcpStreamMetrics;
    private final boolean connectIfNotConnected;
    private final ChannelHandlerMethods channelHandlerMethods;
    private SendStreamContinuoslyLogics readStreamSend;
    private Properties properties;



    public StreamingChannel(Properties properties, boolean singleThreaded, ChannelHandlerMethods chm, NetworkRole networkRole)throws IOException{
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
        readStreamSend = null;
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
        List<CustomTCPConnection> connections = addressToConnections.get(connection.host);
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
            logger.info("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            channelHandlerMethods.onChannelReadDelimitedMessage(connection.conId,bytes,connection.host);
        }
    }
    public void onChannelStreamRead(String channelId, BabelInBytesWrapper babelInBytesWrapper){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.info("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            channelHandlerMethods.onChannelReadFlowStream(connection.conId,babelInBytesWrapper,connection.host);
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
            CustomTCPConnection connection = new CustomTCPConnection(channel,type,listeningAddress,conId,inConnection);
            nettyIdToConnection.put(channel.id().asShortText(),connection);
            List<CustomTCPConnection> cons = addressToConnections.get(listeningAddress);
            if(cons == null){
                cons = new LinkedList<>();
                addressToConnections.put(listeningAddress,cons);
            }
            cons.add(connection);
            customIdToConnection.put(conId,connection);
            if(metricsOn){
                tcpStreamMetrics.updateConnectionMetrics(channel.remoteAddress(),listeningAddress,inConnection);
            }
            sendPendingMessages(connection,type);
            //    void onConnectionUp(boolean incoming, InetSocketAddress peer, TransmissionType type, String customConId);
            channelHandlerMethods.onConnectionUp(connection.inConnection,connection.host,type,conId);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }


    public void onConnectionFailed(String channelId, Throwable cause){
        CustomTCPConnection customTCPConnection = nettyIdToConnection.get(channelId);
        channelInactiveLogics(channelId);
        if(customTCPConnection != null){
            handleOpenConnectionFailed(customTCPConnection.host,cause);
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
    public String openConnectionLogics(InetSocketAddress peer, TransmissionType type, String conId) {
        if(conId == null ){
            conId = nextId();
        }
        nettyIdTOConnectingOBJ.put(conId,new TCPConnectingObject(conId,peer,new LinkedList<>()));
        logger.debug("{} CONNECTING TO {}",self,peer);
        try {
            client.connect(peer,tcpStreamMetrics,this,type,conId);
            return conId;
        }catch (Exception e){
            e.printStackTrace();
            handleOpenConnectionFailed(peer,e.getCause());
        }
        return null;
    }
    public void closeConnection(InetSocketAddress peer) {
        logger.info("CLOSING CONNECTION TO {}", peer);
        List<CustomTCPConnection> connections = addressToConnections.remove(peer);
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
        connection.close();
    }



    private void sendPendingMessages(CustomTCPConnection customTCPConnection, TransmissionType type){
        TCPConnectingObject connectingObject = nettyIdTOConnectingOBJ.remove(customTCPConnection.conId);
        if(connectingObject!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self
                    ,connectingObject.pendingMessages.size(),customTCPConnection.host);
            for (Pair<byte[],Integer> message : connectingObject.pendingMessages) {
                send(customTCPConnection.host,message.getLeft(),message.getRight(),type);
            }
        }
    }
    public void closeServerSocket(){
        server.closeServerSocket();
    }
    public void send(String customConId, byte[] message, int len,TransmissionType type){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onMessageSent(message, null, len,new Throwable("Unknown Connection ID : "+customConId),null,type);
        }else{
            send(message,len,connection,type);
        }
    }
    private void send(byte[] message, int len, CustomTCPConnection connection, TransmissionType type){
        if(connection.type==type){
            ByteBuf byteBuf;
            if(TransmissionType.UNSTRUCTURED_STREAM==type){
                byteBuf = Unpooled.buffer(len);
            }else{
                byteBuf = Unpooled.buffer(len+4);
                byteBuf.writeInt(len);
            }
            byteBuf.writeBytes(message,0,len);
            ChannelFuture f = connection.channel.writeAndFlush(byteBuf);
            f.addListener(future -> {
                if(future.isSuccess()){
                    if(metricsOn){
                        TCPStreamConnectionMetrics metrics1 = tcpStreamMetrics.getConnectionMetrics(f.channel().remoteAddress());
                        metrics1.setSentAppBytes(metrics1.getSentAppBytes()+message.length);
                        metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
                    }
                }
                //    void onMessageSent(byte[] message, InputStream inputStream, int len, Throwable error, InetSocketAddress peer, TransmissionType type);
                channelHandlerMethods.onMessageSent(message,null,len,future.cause(),connection.host,type);
            });
        }else{
            channelHandlerMethods.onMessageSent(message, null,len,new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND RECEIVED "+type+" DATA TYPE"),connection.host,type);
        }
    }
    public CustomTCPConnection getConnection(InetSocketAddress peer){
        List<CustomTCPConnection> connections = addressToConnections.get(peer);
        if(connections!=null&&!connections.isEmpty()){
            return connections.get(0);
        }
        return null;
    }
    public void sendInputStream(InputStream inputStream, int len, InetSocketAddress peer, String conId)  {
        try {
            CustomTCPConnection idConnection = customIdToConnection.get(conId);
            CustomTCPConnection peerConnection=null;
            if(idConnection == null && (peerConnection = getConnection(peer))==null ){
                //channelHandlerMethods.onMessageSent(null,inputStream,len,t,peer,TransmissionType.STRUCTURED_MESSAGE);
                channelHandlerMethods.onMessageSent(null,inputStream,len,new Throwable("FAILED TO SEND INPUTSTREAM. UNKNOWN PEER AND CONID: "+peer+" - "+conId),peer,null);
                return;
            }else if(peerConnection != null ){
                idConnection = peerConnection;
            }
            if(idConnection.type!=TransmissionType.UNSTRUCTURED_STREAM){
                Throwable t = new Throwable("INPUTSTREAM CAN ONLY BE SENT WITH UNSTRUCTURED STREAM TRANSMISSION TYPE");
                //channelHandlerMethods.onMessageSent(message,null,len,future.cause(),connection.host,type);
                channelHandlerMethods.onMessageSent(null,inputStream,len,t,peer,TransmissionType.STRUCTURED_MESSAGE);
                return;
            }

            FileInputStream in = (FileInputStream) inputStream;
            FileRegion region = new DefaultFileRegion(in.getChannel(), 0, inputStream.available());
            ChannelFuture c = idConnection.channel.writeAndFlush(region);


            CustomTCPConnection finalIdConnection = idConnection;
            c.addListener(future -> {
                channelHandlerMethods.onMessageSent(null,inputStream,len,future.cause(),finalIdConnection.host,TransmissionType.UNSTRUCTURED_STREAM);
            });
        }catch (Exception e){
            channelHandlerMethods.onMessageSent(null,inputStream,len,e.getCause(),peer,TransmissionType.UNSTRUCTURED_STREAM);
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
    public void send( InetSocketAddress peer, byte[] message, int len, TransmissionType type){
        var connections = addressToConnections.get(peer);
        CustomTCPConnection connection;
        if(connections == null || (connection=connections.get(0)) == null){
            TCPConnectingObject pendingMessages  = connectingObject(peer);
            if( pendingMessages != null ){
                pendingMessages.pendingMessages.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                String conId = openConnectionLogics(peer,type,null);
                nettyIdTOConnectingOBJ.get(conId).pendingMessages.add(Pair.of(message,len));
            }else{
                channelHandlerMethods.onMessageSent(message,null,len,new Throwable("Unknown Peer : "+peer),peer,type);
            }
        }else {
            send(message,len,connection,type);
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
    public InetSocketAddress[] getAddressToQUICCons() {
        return addressToConnections.keySet().toArray(new InetSocketAddress[addressToConnections.size()]);
    }

    @Override
    public int connectedPeers() {
        return addressToConnections.size();
    }

    @Override
    public String[] getStreams() {
        return customIdToConnection.keySet().toArray(new String[customIdToConnection.size()]);
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
        List<CustomTCPConnection> cons = addressToConnections.get(peer);
        if(cons==null || cons.isEmpty()){
            return null;
        }else{
            return cons.get(0).type;
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
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause){
        channelHandlerMethods.onOpenConnectionFailed(peer,cause);
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
