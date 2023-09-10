package tcpSupport.tcpChannelAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FileRegion;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.channels.NettyChannelInterface;
import quicSupport.channels.SendStreamInterface;
import quicSupport.utils.enums.NetworkProtocol;
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


public class NettyTCPChannel implements StreamingNettyConsumer, NettyChannelInterface, SendStreamInterface {
    private static final Logger logger = LogManager.getLogger(NettyTCPChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";
    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String NOT_ZERO_COPY = "NOT_ZERO_COPY";

    public final static String DEFAULT_PORT = "8574";
    private final Map<String, CustomTCPConnection> nettyIdToConnection;
    private final Map<String, TCPConnectingObject> nettyIdTOConnectingOBJ;
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

    private final BabelMessageSerializer serializer;
    private final EventLoopGroup serverParentGroup;

    public NettyTCPChannel(Properties properties, boolean singleThreaded, ChannelHandlerMethods chm, NetworkRole networkRole, BabelMessageSerializer serializer)throws IOException{
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
            server = new TCPServerEntity(addr.getHostName(),port,this,properties);
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
        serverParentGroup = setGroup(client,server, networkRole);
        connectIfNotConnected = properties.getProperty(TCPChannelUtils.AUTO_CONNECT_ON_SEND_PROP)!=null;
        singleConnectionPerPeer = properties.getProperty(TCPChannelUtils.SINGLE_CON_PER_PEER)!=null;
        this.channelHandlerMethods = chm;
        streamContinuoslyLogics = null;
        this.properties = properties;
    }

    public static EventLoopGroup setGroup(ClientInterface client, ServerInterface server, NetworkRole networkRole){
        if(NetworkRole.SERVER==networkRole||NetworkRole.P2P_CHANNEL==networkRole){
            return server.getEventLoopGroup();
        }else{
            return client.getEventLoopGroup();
        }
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

    public void onChannelMessageRead(String channelId, BabelMessage babelMessage, int bytes){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.debug("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            calcMetricsOnReceived(connection.conId,bytes+Integer.BYTES);
            channelHandlerMethods.onChannelReadDelimitedMessage(connection.conId,babelMessage,connection.host);
        }
    }
    public void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream){
        CustomTCPConnection connection = nettyIdToConnection.get(channelId);
        if(connection==null){
            logger.debug("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else {
            calcMetricsOnReceived(connection.conId,babelOutputStream.readableBytes());
            channelHandlerMethods.onChannelReadFlowStream(connection.conId,babelOutputStream,connection.host,connection.inputStream,connection.streamProto);
        }
    }
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, int len){
        try {
            boolean inConnection;
            InetSocketAddress listeningAddress;
            String conId;
            short streamProto;
            if(handShakeMessage==null){//out connection
                listeningAddress = (InetSocketAddress) channel.remoteAddress();
                inConnection = false;
                conId = channel.attr(AttributeKey.valueOf(TCPChannelUtils.CUSTOM_ID_KEY)).getAndSet(null).toString();
                streamProto = nettyIdTOConnectingOBJ.get(conId).streamProto;
            }else {//in connection
                listeningAddress = handShakeMessage.getAddress();
                inConnection = true;
                conId = nextId();
                streamProto  = handShakeMessage.destProto;
            }

            if(metricsManager !=null){
                metricsManager.initConnectionMetrics(conId,listeningAddress,inConnection,len, type);
            }

            BabelInputStream babelInputStream = BabelInputStream.toBabelStream(conId,this,type, channel.alloc());
            CustomTCPConnection connection = new CustomTCPConnection(channel,type,listeningAddress,conId,inConnection,babelInputStream,streamProto);
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
        TCPConnectingObject connectingObject = nettyIdTOConnectingOBJ.remove(customTCPConnection.conId);
        if(connectingObject!=null && STRUCTURED_MESSAGE==type){
            /*logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self
                    ,connectingObject.pendingMessages.size(),customTCPConnection.host);*/
            for (BabelMessage message : connectingObject.pendingMessages) {
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
    public String open(InetSocketAddress peer, TransmissionType type, short sourceProto, short destProto, boolean always) {
        return openConnectionLogics(peer,type,null,sourceProto,destProto,always);
    }
    private String isConnecting(InetSocketAddress peer){
        for (TCPConnectingObject value : nettyIdTOConnectingOBJ.values()) {
            if(value.dest.equals(peer)){
                return value.customId;
            }
        }
        return null;
    }
    public String openConnectionLogics(InetSocketAddress peer, TransmissionType type, String conId, short sourceProto, short destProto, boolean always) {
        boolean singleConPerPeer = (always==false);
        if(singleConPerPeer){
            String connectionId = isConnecting(peer);
            if(connectionId!=null){
                logger.info("Opening more than one connection to a connected peer when <always> open flag is false!");
                return connectionId;
            }
            try{
                CustomTCPConnection con = addressToConnections.get(peer).peek();
                logger.info("Opening more than one connection to a connected peer when <always> open flag is false!");
                channelHandlerMethods.onConnectionUp(con.inConnection,peer,con.type,con.conId,con.inputStream);
                return con.conId;
            }catch (Exception e){};
        }
        if(conId == null ){
            conId = nextId();
        }
        nettyIdTOConnectingOBJ.put(conId,new TCPConnectingObject(conId,peer,new LinkedList<>(),sourceProto));
        logger.debug("{} CONNECTING TO {}. CONNECTION ID: ",self,peer,conId);
        try {
            client.connect(peer,type,conId,destProto);
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
    public void send(String customConId, BabelMessage message){
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
    private void calcMetricsOnSend(String connectionId, long length){
        if(enabledMetrics()){
            metricsManager.calcMetricsOnSend(true,connectionId,length);
        }
    }

    private void sendAux(BabelMessage message, CustomTCPConnection connection){
        serverParentGroup.next().execute(() -> {
            try{
                if(connection.type== STRUCTURED_MESSAGE){

                    ByteBuf byteBuf = connection.channel.alloc().directBuffer().writeInt(0);
                    serializer.serialize(message,byteBuf);
                    final int len = byteBuf.readableBytes();
                    byteBuf.setInt(0,len-Integer.BYTES);
                    connection.channel.writeAndFlush(byteBuf);
                    calcMetricsOnSend(connection.conId,len);
                    channelHandlerMethods.onMessageSent(message,null,connection.host,STRUCTURED_MESSAGE,connection.conId);
                    /**
                    f.addListener(future -> {
                        });**/
                }else{
                    channelHandlerMethods.onMessageSent(message,new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND EXPECTING "+ STRUCTURED_MESSAGE+" DATA TYPE"),connection.host, STRUCTURED_MESSAGE,connection.conId);
                }
            }catch (Exception e){
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public void sendStream(String customConId,ByteBuf byteBuf,boolean flush){
        CustomTCPConnection connection = customIdToConnection.get(customConId);
        if(connection == null ){
            channelHandlerMethods.onStreamDataSent(null,new byte[0],byteBuf.readableBytes(),new Throwable("Unknown Connection ID : "+customConId),null,TransmissionType.UNSTRUCTURED_STREAM,customConId);
        }else{
            serverParentGroup.next().execute(() -> {
                final int toSend = byteBuf.readableBytes();
                //ChannelFuture f;
                if(flush){
                    connection.channel.writeAndFlush(byteBuf);
                }else{
                    connection.channel.write(byteBuf);
                }
                calcMetricsOnSend(connection.conId,toSend);
            });
            //channelHandlerMethods.onStreamDataSent(null,new byte[0],toSend,future.cause(),connection.host,TransmissionType.UNSTRUCTURED_STREAM,customConId);
            /**
            f.addListener(future -> {
                });**/
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
        CustomTCPConnection idConnection = customIdToConnection.get(conId);
        if(idConnection == null){
            channelHandlerMethods.onStreamDataSent(inputStream,null,-1, new Throwable("FAILED TO SEND INPUTSTREAM. UNKNOWN CONID: "+conId),null,null,conId);
            return;
        }
        if(idConnection.type!=TransmissionType.UNSTRUCTURED_STREAM){
            Throwable t = new Throwable("INPUTSTREAM CAN ONLY BE SENT WITH UNSTRUCTURED STREAM TRANSMISSION TYPE. CON ID:"+conId);
            channelHandlerMethods.onStreamDataSent(inputStream,null,-1,t,idConnection.host, STRUCTURED_MESSAGE,conId);
            return;
        }
        serverParentGroup.next().execute(() -> {
            if(len<=0){
                if(streamContinuoslyLogics==null)streamContinuoslyLogics = new SendStreamContinuoslyLogics(this,properties.getProperty(TCPChannelUtils.READ_STREAM_PERIOD_KEY));
                streamContinuoslyLogics.addToStreams(inputStream,idConnection.conId,serverParentGroup.next());
                return;
            }
            if( properties.getProperty(NOT_ZERO_COPY) == null && inputStream instanceof FileInputStream){
                FileInputStream in = (FileInputStream) inputStream;
                FileRegion region = new DefaultFileRegion(in.getChannel(), 0,len);
                idConnection.channel.writeAndFlush(region);
            }else{
                if(idConnection.channel.pipeline().get("ChunkedWriteHandler")==null){
                    idConnection.channel.pipeline().addLast("ChunkedWriteHandler",new ChunkedWriteHandler());
                }
                idConnection.channel.writeAndFlush(new ChunkedStream(inputStream));
            }
            calcMetricsOnSend(conId,len);
        });
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
    public void send(InetSocketAddress peer,BabelMessage message){
        CustomTCPConnection connection=getConnection(peer);

        if(connection == null){
            TCPConnectingObject pendingMessages  = connectingObject(peer);
            if( pendingMessages != null ){
                pendingMessages.pendingMessages.add(message);
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                short p = -1;
                String conId = openConnectionLogics(peer,STRUCTURED_MESSAGE,null,p,p,false);
                nettyIdTOConnectingOBJ.get(conId).pendingMessages.add(message);
            }else{
                channelHandlerMethods.onMessageSent(message,new Throwable("Unknown Peer : "+peer),peer, STRUCTURED_MESSAGE,null);
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

    @Override
    public NetworkProtocol getNetworkProtocol() {
        return NetworkProtocol.TCP;
    }

    public void channelError(InetSocketAddress address, Throwable throwable, String nettyID){
        CustomTCPConnection customTCPConnection = nettyIdToConnection.get(nettyID);
        if(customTCPConnection!=null){
            address = customTCPConnection.host;
            nettyID = customTCPConnection.conId;
        }else{
            nettyID = null;
        }
        channelHandlerMethods.onStreamErrorHandler(address,throwable,nettyID);
    }

    @Override
    public BabelMessageSerializer getSerializer() {
        return serializer;
    }
}
