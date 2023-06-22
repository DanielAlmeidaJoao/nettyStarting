package tcpSupport.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.connectionSetups.StreamInConnection;
import tcpSupport.tcpStreamingAPI.connectionSetups.StreamOutConnection;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPStreamMetrics;
import tcpSupport.tcpStreamingAPI.utils.MetricsDisabledException;
import tcpSupport.tcpStreamingAPI.utils.TCPConnectingObject;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;


public class StreamingChannel implements StreamingNettyConsumer, TCPChannelInterface{
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
    private final TCPChannelHandlerMethods channelHandlerMethods;


    public StreamingChannel(Properties properties, boolean singleThreaded, TCPChannelHandlerMethods chm, NetworkRole networkRole)throws IOException{
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
        channelHandlerMethods.onChannelInactive(connection.host);
    }

    public void onChannelRead(String channelId, byte[] bytes, TransmissionType type){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.info("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else if(TransmissionType.STRUCTURED_MESSAGE==type){
            channelHandlerMethods.onChannelMessageRead(channelId,bytes,connection.host,connection.conId);
        }else{
            channelHandlerMethods.onChannelStreamRead(channelId,bytes,connection.host,connection.conId);
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
            CustomTCPConnection connection = new CustomTCPConnection(channel,type,listeningAddress,conId);
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
            channelHandlerMethods.onChannelActive(connection.conId,inConnection,listeningAddress,type);
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
    public String openConnection(InetSocketAddress peer, TransmissionType type) {
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
        List<CustomTCPConnection> connections = addressToConnections.get(peer);
        if(connections!=null){
            for (CustomTCPConnection connection : connections) {
                channelInactiveLogics(connection.conId);
                connection.close();
            }
        }
    }

    @Override
    public void closeConnection(String connectionId) {
        CustomTCPConnection connection = customIdToConnection.remove(connectionId);
        connection.close();
    }

    private void sendPendingMessages(CustomTCPConnection customTCPConnection, TransmissionType type){
        TCPConnectingObject connectingObject = nettyIdTOConnectingOBJ.remove(customTCPConnection.conId);
        if(connectingObject!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self
                    ,connectingObject.pendingMessages.size(),customTCPConnection.host);
            for (Pair<byte[],Integer> message : connectingObject.pendingMessages) {
                send(message.getLeft(),message.getRight(),customTCPConnection.host,type);
            }
        }
    }
    public void closeServerSocket(){
        server.closeServerSocket();
    }
    public void send(byte[] message, int len,String customConId, TransmissionType type){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onMessageSent(message,null,new Throwable("Unknown Connection ID : "+customConId),type);
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
                channelHandlerMethods.onMessageSent(message,connection.host,future.cause(), type);
            });
        }else{
            channelHandlerMethods.onMessageSent(message,connection.host,new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND RECEIVED "+type+" DATA TYPE"), type);
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
    public void send(byte[] message, int len, InetSocketAddress peer, TransmissionType type){
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
                channelHandlerMethods.onMessageSent(message,peer,new Throwable("Unknown Peer : "+peer),type);
            }
        }else {
            send(message,len,connection,type);
        }
    }

    @Override
    public boolean isConnected(InetSocketAddress peer) {
        return addressToConnections.containsKey(peer);
    }

    @Override
    public InetSocketAddress[] getNettyIdToConnection() {
        return addressToConnections.keySet().toArray(new InetSocketAddress[addressToConnections.size()]);
    }


    @Override
    public int connectedPeers() {
        return addressToConnections.size();
    }

    @Override
    public String[] getLinks() {
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
    public TransmissionType getConnectionType(InetSocketAddress peer) throws NoSuchElementException{
        List<CustomTCPConnection> cons = addressToConnections.get(peer);
        if(cons==null || cons.isEmpty()){
            throw new NoSuchElementException("UNKNOWN PEER "+peer);
        }else{
            return cons.get(0).type;
        }
    }

    @Override
    public TransmissionType getConnectionStreamTransmissionType(String streamId) throws NoSuchElementException {
        CustomTCPConnection connection = customIdToConnection.get(streamId);
        if(connection==null){
            throw new NoSuchElementException("NO SUCH CONNECTION ID: "+streamId);
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

    protected void readMetrics(ReadMetricsHandler handler) throws MetricsDisabledException {
        if(metricsOn){
            handler.readMetrics(tcpStreamMetrics.currentMetrics(),tcpStreamMetrics.oldMetrics());
        }else {
            throw new MetricsDisabledException("METRICS WAS NOT ENABLED!");
        }
    }
}
