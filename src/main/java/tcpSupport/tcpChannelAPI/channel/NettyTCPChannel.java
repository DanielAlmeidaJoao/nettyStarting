package tcpSupport.tcpChannelAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.NettyChannelInterface;
import quicSupport.channels.SendStreamInterface;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.connectionSetups.*;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetrics;
import tcpSupport.tcpChannelAPI.metrics.ConnectionProtocolMetricsManager;
import tcpSupport.tcpChannelAPI.utils.*;

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
import java.util.concurrent.ConcurrentLinkedQueue;

import static quicSupport.utils.enums.TransmissionType.STRUCTURED_MESSAGE;


public class NettyTCPChannel<T> implements StreamingNettyConsumer, NettyChannelInterface<T>, SendStreamInterface {
    private static final Logger logger = LogManager.getLogger(NettyTCPChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";
    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String ZERO_COPY = "ZERO_COPY";

    public final static String DEFAULT_PORT = "8574";
    private final Map<String, CustomTCPConnection> nettyIdToConnection;
    private final Map<String, TCPConnectingObject<T>> nettyIdTOConnectingOBJ;
    private final Map<InetSocketAddress, ConcurrentLinkedQueue<CustomTCPConnection>> addressToConnections;
    private final Map<String,CustomTCPConnection> customIdToConnection;
    private final ServerInterface server;
    private final ClientInterface client;
    private final ConnectionProtocolMetricsManager metricsManager;
    private final boolean connectIfNotConnected;
    private final boolean singleConnectionPerPeer;
    private final ChannelHandlerMethods channelHandlerMethods;
    private SendStreamContinuoslyLogics streamContinuoslyLogics;
    private Properties properties;
    public final NetworkRole networkRole;

    private final BabelMessageSerializerInterface<T> serializer;

    public NettyTCPChannel(Properties properties, boolean singleThreaded, ChannelHandlerMethods chm, NetworkRole networkRole,BabelMessageSerializerInterface<T> serializer)throws IOException{
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        this.serializer = serializer;
        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        boolean metricsOn = properties.containsKey(TCPChannelUtils.CHANNEL_METRICS);
        if(metricsOn){
            metricsManager = new ConnectionProtocolMetricsManager(self,singleThreaded);
        }else{
            metricsManager = null;
        }
        nettyIdToConnection = TCPChannelUtils.getMapInst(singleThreaded);
        addressToConnections = TCPChannelUtils.getMapInst(singleThreaded);
        nettyIdTOConnectingOBJ = TCPChannelUtils.getMapInst(singleThreaded);
        customIdToConnection = TCPChannelUtils.getMapInst(singleThreaded);
        this.networkRole = networkRole;
        if(NetworkRole.P2P_CHANNEL ==networkRole||NetworkRole.SERVER==networkRole){
            server = new TCPServerEntity(addr.getHostName(),port,this);
            try{
                server.startServer();
            }catch (Exception e){
                throw new IOException(e);
            }
        }else{
            server = new DummyServer();
        }
        if(NetworkRole.P2P_CHANNEL ==networkRole||NetworkRole.CLIENT==networkRole){
            if(NetworkRole.CLIENT==networkRole){
                properties.remove(TCPChannelUtils.AUTO_CONNECT_ON_SEND_PROP);
            }
            client = new TCPClientEntity(self,properties,this);
        }else{
            client=new DummyClient();
        }

        connectIfNotConnected = properties.getProperty(TCPChannelUtils.AUTO_CONNECT_ON_SEND_PROP)!=null;
        singleConnectionPerPeer = properties.getProperty(TCPChannelUtils.SINGLE_CON_PER_PEER)!=null;
        this.channelHandlerMethods = chm;
        streamContinuoslyLogics = null;
        this.properties = properties;
    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public  void onChannelInactive(String channelId){
        channelInactiveLogics(channelId);
    }
    private void channelInactiveLogics(String nettyId){
        nettyIdTOConnectingOBJ.remove(nettyId);
        CustomTCPConnection connection = nettyIdToConnection.remove(nettyId);
        if(connection == null){
            return;
        }
        customIdToConnection.remove(connection.conId);
        ConcurrentLinkedQueue<CustomTCPConnection> connections = addressToConnections.get(connection.host);
        if(connections!=null && connections.remove(connection) && connections.isEmpty()){
            addressToConnections.remove(connection.host);
        }
        if(enabledMetrics()){
            metricsManager.onConnectionClosed(connection.conId);
        }
        channelHandlerMethods.onStreamClosedHandler(connection.host,connection.conId,connection.inConnection,connection.type);
    }

    public void onChannelMessageRead(String channelId, ByteBuf bytes){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.debug("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            calcMetricsOnReceived(connection.conId,bytes.readableBytes()+Integer.BYTES);
            try {
                T babelMessage = serializer.deserialize(bytes);
                channelHandlerMethods.onChannelReadDelimitedMessage(connection.conId,babelMessage,connection.host);
                //FactoryMethods.deserialize(bytes,serializer,listener,from,connectionId);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    public void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.debug("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            calcMetricsOnReceived(connection.conId,babelOutputStream.readableBytes());
            channelHandlerMethods.onChannelReadFlowStream(connection.conId,babelOutputStream,connection.host,connection.inputStream);
        }
    }
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, int len){
        try {
            boolean inConnection;
            InetSocketAddress listeningAddress;
            String conId;
            if(handShakeMessage==null){//out connection
                listeningAddress = (InetSocketAddress) channel.remoteAddress();
                inConnection = false;
                conId = channel.attr(AttributeKey.valueOf(TCPChannelUtils.CUSTOM_ID_KEY)).toString();
            }else {//in connection
                listeningAddress = handShakeMessage.getAddress();
                inConnection = true;
                conId = nextId();
            }

            if(metricsManager !=null){
                metricsManager.initConnectionMetrics(conId,listeningAddress,inConnection,len, type);
            }

            BabelInputStream babelInputStream = BabelInputStream.toBabelStream(conId,this,type, channel.alloc());
            CustomTCPConnection connection = new CustomTCPConnection(channel,type,listeningAddress,conId,inConnection,babelInputStream);
            nettyIdToConnection.put(channel.id().asShortText(),connection);
            customIdToConnection.put(conId,connection);
            synchronized (this){
                addressToConnections.computeIfAbsent(listeningAddress, k -> new ConcurrentLinkedQueue<>()).add(connection);;
            }
            sendPendingMessages(connection,type);
            channelHandlerMethods.onConnectionUp(connection.inConnection,connection.host,type,conId, babelInputStream);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }
    private void sendPendingMessages(CustomTCPConnection customTCPConnection, TransmissionType type){
        TCPConnectingObject<T> connectingObject = nettyIdTOConnectingOBJ.remove(customTCPConnection.conId);
        if(connectingObject!=null && STRUCTURED_MESSAGE==type){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self
                    ,connectingObject.pendingMessages.size(),customTCPConnection.host);
            for (T message : connectingObject.pendingMessages) {
                sendAux(message,customTCPConnection);
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
        return "tcpChan"+ TCPChannelUtils.channelIdCounter.getAndIncrement();
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
        if(singleConnectionPerPeer){
            String connectionId = isConnecting(peer);
            if(connectionId!=null) return connectionId;
            try{
                CustomTCPConnection con = addressToConnections.get(peer).peek();
                //channelHandlerMethods.onConnectionUp(con.inConnection,peer,con.type,con.conId,con.inputStream);
                return con.conId;
            }catch (Exception e){};
        }
        if(conId == null ){
            conId = nextId();
        }
        nettyIdTOConnectingOBJ.put(conId,new TCPConnectingObject(conId,peer,new LinkedList<>()));
        logger.debug("{} CONNECTING TO {}. CONNECTION ID: ",self,peer,conId);
        try {
            client.connect(peer,type,conId);
            return conId;
        }catch (Exception e){
            e.printStackTrace();
            handleOpenConnectionFailed(peer,e.getCause(), type,conId);
        }
        return null;
    }
    public void closeConnection(InetSocketAddress peer) {
        logger.debug("CLOSING ALL CONNECTIONS TO {}", peer);
        ConcurrentLinkedQueue<CustomTCPConnection> connections = addressToConnections.remove(peer);
        if(connections!=null){
            for (CustomTCPConnection connection : connections) {
                connection.close();
            }
        }
    }

    @Override
    public void closeLink(String connectionId) {
        CustomTCPConnection connection = customIdToConnection.remove(connectionId);
        if(connection!=null){
            connection.close();
        }
    }
    public void closeServerSocket(){
        server.shutDown();
    }
    public void send(String customConId, T message){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onMessageSent(message,new Throwable("Unknown Connection ID : "+customConId),null, STRUCTURED_MESSAGE,customConId);
        }else{
            sendAux(message,connection);
        }
    }
    private void calcMetricsOnReceived(String conId,long bytes){
        if(enabledMetrics()){
            metricsManager.calcMetricsOnReceived(conId,bytes);
        }
    }
    private void calcMetricsOnSend(Future future, String connectionId, long length){
        if(future.isSuccess()){
            if(enabledMetrics()){
                metricsManager.calcMetricsOnSend(future.isSuccess(),connectionId,length);
            }
        }
    }
    private void sendAux(T message, CustomTCPConnection connection){
        try{
            if(connection.type== STRUCTURED_MESSAGE){

                ByteBuf byteBuf = connection.channel.alloc().directBuffer().writeInt(0);
                serializer.serialize(message,byteBuf);
                final int len = byteBuf.readableBytes();
                byteBuf.setInt(0,len-Integer.BYTES);
                ChannelFuture f = connection.channel.writeAndFlush(byteBuf);
                f.addListener(future -> {
                    calcMetricsOnSend(future,connection.conId,len);
                    channelHandlerMethods.onMessageSent(message,future.cause(),connection.host,STRUCTURED_MESSAGE,connection.conId);
                });
            }else{
                channelHandlerMethods.onMessageSent(message,new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND EXPECTING "+ STRUCTURED_MESSAGE+" DATA TYPE"),connection.host, STRUCTURED_MESSAGE,connection.conId);
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void sendStream(String customConId,ByteBuf byteBuf,boolean flush){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onStreamDataSent(null,new byte[0],byteBuf.readableBytes(),new Throwable("Unknown Connection ID : "+customConId),null,TransmissionType.UNSTRUCTURED_STREAM,customConId);
        }else{
            final int toSend = byteBuf.readableBytes();
            ChannelFuture f;
            if(flush){
                f = connection.channel.writeAndFlush(byteBuf);
            }else{
                f = connection.channel.write(byteBuf);
            }
            f.addListener(future -> {
                calcMetricsOnSend(future,connection.conId,toSend);
                channelHandlerMethods.onStreamDataSent(null,new byte[0],toSend,future.cause(),connection.host,TransmissionType.UNSTRUCTURED_STREAM,customConId);
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
                channelHandlerMethods.onStreamDataSent(inputStream,null,inputStream.available(), new Throwable("FAILED TO SEND INPUTSTREAM. UNKNOWN CONID: "+conId),null,null,conId);
                return;
            }
            if(idConnection.type!=TransmissionType.UNSTRUCTURED_STREAM){
                Throwable t = new Throwable("INPUTSTREAM CAN ONLY BE SENT WITH UNSTRUCTURED STREAM TRANSMISSION TYPE. CON ID:"+conId);
                channelHandlerMethods.onStreamDataSent(inputStream,null,inputStream.available(),t,idConnection.host, STRUCTURED_MESSAGE,conId);
                return;
            }
            if(len<=0){
                if(streamContinuoslyLogics==null)streamContinuoslyLogics = new SendStreamContinuoslyLogics(this,properties.getProperty(TCPChannelUtils.READ_STREAM_PERIOD_KEY));
                streamContinuoslyLogics.addToStreams(inputStream,idConnection.conId,idConnection.channel.eventLoop());
                return;
            }
            ChannelFuture c;

            if( properties.getProperty(ZERO_COPY) !=null && inputStream instanceof FileInputStream){
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
                calcMetricsOnSend(future,conId,len);
                channelHandlerMethods.onStreamDataSent(inputStream,null,(int) len, future.cause(),finalIdConnection.host,TransmissionType.UNSTRUCTURED_STREAM,conId);
            });
        }catch (Exception e){
            channelHandlerMethods.onStreamDataSent(inputStream,null,(int) len,e.getCause(),null,TransmissionType.UNSTRUCTURED_STREAM,conId);
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
    private CustomTCPConnection getConnection(InetSocketAddress peer){
        var connections = addressToConnections.get(peer);
        if(connections!=null){
            for (CustomTCPConnection connection : connections) {
                if(STRUCTURED_MESSAGE==connection.type){
                    return connection;
                }
            }
        }
        return null;
    }
    public void send(InetSocketAddress peer,T message){
        CustomTCPConnection connection=getConnection(peer);

        if(connection == null){
            TCPConnectingObject pendingMessages  = connectingObject(peer);
            if( pendingMessages != null ){
                pendingMessages.pendingMessages.add(message);
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                String conId = openConnectionLogics(peer,STRUCTURED_MESSAGE,null);
                nettyIdTOConnectingOBJ.get(conId).pendingMessages.add(message);
            }else{
                channelHandlerMethods.onMessageSent(message,new Throwable("Unknown Peer : "+peer),peer, STRUCTURED_MESSAGE,connection.conId);
            }
        }else {
            sendAux(message,connection);
        }
    }

    @Override
    public boolean enabledMetrics() {
        return metricsManager != null;
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
            server.shutDown();
        }
        if(client!=null){
            client.shutDown();
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

    @Override
    public boolean isConnected(String connectionID) {
        return customIdToConnection.containsKey(connectionID);
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

    public void readMetrics(ReadMetricsHandler handler) {
        if(enabledMetrics()){
            handler.readMetrics(metricsManager.currentMetrics(), metricsManager.oldMetrics());
        }else {
            logger.error("METRICS NOT ENABLED!");
            handler.readMetrics(null,null);
        }
    }

    public NetworkRole getNetworkRole(){
        return networkRole;
    }

    @Override
    public List<ConnectionProtocolMetrics> currentMetrics() {
        return metricsManager == null ? null : metricsManager.currentMetrics();
    }

    @Override
    public List<ConnectionProtocolMetrics> oldMetrics() {
        return metricsManager == null ? null : metricsManager.oldMetrics();
    }
}
