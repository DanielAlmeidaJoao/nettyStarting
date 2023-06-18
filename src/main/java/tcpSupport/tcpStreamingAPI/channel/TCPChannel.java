package tcpSupport.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tcpSupport.tcpStreamingAPI.connectionSetups.TCPInConnection;
import tcpSupport.tcpStreamingAPI.connectionSetups.TCPOutConnection;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpStreamingAPI.metrics.TCPSConnectionMetrics;
import tcpSupport.tcpStreamingAPI.metrics.TCPMetrics;
import tcpSupport.tcpStreamingAPI.utils.MetricsDisabledException;
import tcpSupport.tcpStreamingAPI.utils.TCPStreamUtils;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class TCPChannel implements TCPNettyConsumer, TCPChannelInterface{
    private static final Logger logger = LogManager.getLogger(TCPChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private final Map<InetSocketAddress, CustomTCPConnection> connections;
    private final Map<String,Pair<InetSocketAddress,List<Pair<byte [],Integer>>>> connecting;

    private final Map<String,CustomTCPConnection> links;


    private final TCPInConnection server;
    private final TCPOutConnection client;
    private final boolean metricsOn;
    private final TCPMetrics tcpMetrics;
    private final boolean connectIfNotConnected;
    private final TCPChannelHandlerMethods channelHandlerMethods;

    private final AtomicInteger idCounterGenerator;


    public TCPChannel(Properties properties, boolean singleThreaded, TCPChannelHandlerMethods chm, NetworkRole networkRole)throws IOException{
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        metricsOn = properties.containsKey("metrics");
        if(metricsOn){
            tcpMetrics = new TCPMetrics(self,singleThreaded);
        }else{
            tcpMetrics = null;
        }
        if(singleThreaded){
            connections = new HashMap<>();
            connecting=new HashMap<>();
            links = new HashMap<>();
        }else{
            connections = new ConcurrentHashMap<>();
            connecting= new ConcurrentHashMap<>();
            links = new ConcurrentHashMap<>();
        }
        if(NetworkRole.CHANNEL==networkRole||NetworkRole.SERVER==networkRole){
            server = new TCPInConnection(addr.getHostName(),port);
            try{
                server.startListening(false, tcpMetrics,this);
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
            client = new TCPOutConnection(self);
        }else{
            client=null;
        }

        connectIfNotConnected = properties.getProperty(TCPStreamUtils.AUTO_CONNECT_ON_SEND_PROP)!=null;

        this.channelHandlerMethods = chm;
        idCounterGenerator = new AtomicInteger();
    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public String nextId(){
        return String.format("tcp_link_",idCounterGenerator.getAndIncrement());
    }
    public  void onChannelInactive(Pair<InetSocketAddress, String> connectionId){
        CustomTCPConnection chan = links.remove(connectionId.getRight());
        if(chan != null ){
            if(metricsOn){
                tcpMetrics.onConnectionClosed(chan.channel.remoteAddress());
            }
            CustomTCPConnection defaultCon = connections.get(connectionId.getKey());
            if(chan == defaultCon){
                connections.remove(connectionId.getKey());
                String nextDefault;
                if ( (nextDefault = chan.getAndRemove()) != null){
                    CustomTCPConnection newDefault = links.get(nextDefault);
                    connections.put(connectionId.getKey(),newDefault);
                    newDefault.setQueue(defaultCon.getQueue());
                }
            }else{
                defaultCon.removeOtherConnections(connectionId.getRight());
            }
        }
        channelHandlerMethods.onChannelInactive(connectionId.getKey(),connectionId.getRight());
    }

    public void onChannelRead(Pair<InetSocketAddress, String> identification, byte[] bytes, TransmissionType type){
        if(TransmissionType.STRUCTURED_MESSAGE==type){
            channelHandlerMethods.onChannelMessageRead(identification.getValue(),bytes,identification.getKey());
        }else{
            channelHandlerMethods.onChannelStreamRead(identification.getValue(),bytes,identification.getKey());
        }
    }

    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, Pair<InetSocketAddress, String> identification){
        try {
            boolean inConnection;
            InetSocketAddress listeningAddress;
            if(handShakeMessage==null){//out connection
                listeningAddress = (InetSocketAddress) channel.remoteAddress();
                inConnection = false;
            }else {//in connection
                listeningAddress = handShakeMessage.getAddress();
                inConnection = true;
            }
            CustomTCPConnection customTCPConnection = new CustomTCPConnection(channel,type,identification);
            if(connections.containsKey(listeningAddress)){
                CustomTCPConnection c = connections.get(identification.getLeft());
                if(c != null){
                    c.addOtherConnections(identification.getRight());
                }
            }else{
                connections.put(listeningAddress,customTCPConnection);
            }
            links.put(identification.getRight(),customTCPConnection);
            if(metricsOn){
                tcpMetrics.updateConnectionMetrics(channel.remoteAddress(),listeningAddress,inConnection);
            }
            sendPendingMessages(identification.getRight(),type);
            channelHandlerMethods.onChannelActive(identification.getRight(),inConnection,listeningAddress,type);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }


    public void onConnectionFailed(Pair<InetSocketAddress, String> connectionId, Throwable cause){
        handleOpenConnectionFailed(connectionId,cause);
    }

    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/
    public boolean connectIfConnectedOrConnecting = false;
    private boolean isConnectingOrConnected(InetSocketAddress peer){
        if(connections.containsKey(peer)){
            for (Pair<InetSocketAddress, List<Pair<byte[], Integer>>> value : connecting.values()) {
                if(value.getKey().equals(peer)){
                    return true;
                }
            }
        }
        return false;
    }
    public String openLogics(InetSocketAddress peer, TransmissionType type, String connectionId) {
        if(connectionId == null){
            connectionId = nextId();
        }
        if(connections.containsKey(peer)){
            logger.debug("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            //
            if(connectIfConnectedOrConnecting){
                connecting.put(connectionId,Pair.of(peer,new LinkedList<>()));
            }else if(!isConnectingOrConnected(peer)){
                connecting.put(connectionId,Pair.of(peer,new LinkedList<>()));
            }
            logger.debug("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer, tcpMetrics,this,type,connectionId);
            }catch (Exception e){
                e.printStackTrace();
                handleOpenConnectionFailed(Pair.of(peer,connectionId),e.getCause());
            }
        }
        return connectionId;
    }

    @Override
    public String openConnection(InetSocketAddress peer, TransmissionType type) {
        return openLogics(peer, type,null);
    }

    public void closeConnection(InetSocketAddress peer) {
        logger.info("CLOSING CONNECTION TO {}", peer);
        CustomTCPConnection connection = connections.get(peer);
        if(connection.channel!=null){
            connection.channel.close();
        }else {
            logger.info("{} CONNECTION TO {} ALREADY CLOSED",self,peer);
        }
    }
    private void sendPendingMessages(String connectionId, TransmissionType type){
        List<Pair<byte [],Integer>> messages = connecting.remove(connectionId).getRight();
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self,messages.size(),connectionId);
            for (Pair<byte[],Integer> message : messages) {
                send(message.getLeft(),message.getRight(),connectionId,type);
            }
        }
    }
    public void closeServerSocket(){
        server.closeServerSocket();
    }
    private List<Pair<byte[],Integer>> pendingMessages(InetSocketAddress peer){
        for (Pair<InetSocketAddress, List<Pair<byte[], Integer>>> value : connecting.values()) {
            if(value.getKey().equals(peer)){
                return value.getRight();
            }
        }
        return null;
    }
    public void send(byte[] message, int len, InetSocketAddress peer, TransmissionType type){
        CustomTCPConnection connection = connections.get(peer);
        if(connection==null){
            List<Pair<byte[],Integer>> pendingMessages = pendingMessages(peer);
            if( pendingMessages !=null ){
                pendingMessages.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                String connectionId = openLogics(peer, type, null);
                connecting.get(connectionId).getRight().add(Pair.of(message,len));
            }else{
                channelHandlerMethods.onMessageSent(message,peer, connection.id.getValue(), new Throwable("Unknown Peer : "+peer),type);
            }
        }else{
            send(message,len,connection,type);
        }
    }
    public void send(byte[] message, int len, String connectionId, TransmissionType type){
        CustomTCPConnection connection = links.get(connectionId);
        if(connection==null){
            channelHandlerMethods.onMessageSent(message, null/* peer **/, connection.id.getValue(), new Throwable("Unknown Peer : "+null/* peer **/),type);
        }else{
            send(message,len,connection,type);
        }
    }
    private void send(byte[] message, int len,  CustomTCPConnection connection, TransmissionType type){
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
                        TCPSConnectionMetrics metrics1 = tcpMetrics.getConnectionMetrics(f.channel().remoteAddress());
                        metrics1.setSentAppBytes(metrics1.getSentAppBytes()+message.length);
                        metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
                    }
                }
                channelHandlerMethods.onMessageSent(message,connection.id.getKey(),connection.id.getValue(),future.cause(), type);
            });
        }else{
            channelHandlerMethods.onMessageSent(message,connection.id.getKey(),connection.id.getValue(),new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND RECEIVED "+type+" DATA TYPE"), type);
        }
    }

    @Override
    public boolean isConnected(InetSocketAddress peer) {
        return connections.containsKey(peer);
    }

    @Override
    public InetSocketAddress[] getConnections() {
        return connections.keySet().toArray(new InetSocketAddress[connections.size()]);
    }

    @Override
    public int connectedPeers() {
        return connections.size();
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
        CustomTCPConnection connection = connections.get(peer);
        if(connection==null){
            throw new NoSuchElementException("UNKNOWN PEER "+peer);
        }else{
            return connection.type;
        }
    }

    /******************************************* USER EVENTS END ****************************************************/

    public void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }
    public void handleOpenConnectionFailed(Pair<InetSocketAddress, String> peer, Throwable cause){
        connecting.remove(peer.getKey());
        channelHandlerMethods.onOpenConnectionFailed(peer.getKey(),peer.getValue(),cause);
    }

    protected void readMetrics(ReadMetricsHandler handler) throws MetricsDisabledException {
        if(metricsOn){
            handler.readMetrics(tcpMetrics.currentMetrics(), tcpMetrics.oldMetrics());
        }else {
            throw new MetricsDisabledException("METRICS WAS NOT ENABLED!");
        }
    }

    private void debugPrintMetrics(){
        System.out.println("curr: ");
        var ll = tcpMetrics.currentMetrics();
        for (TCPSConnectionMetrics TCPSConnectionMetrics : ll) {
            System.out.println(TCPStreamUtils.g.toJson(TCPSConnectionMetrics));
        }
        System.out.println("old: ");
        ll = tcpMetrics.oldMetrics();
        for (TCPSConnectionMetrics TCPSConnectionMetrics : ll) {
            System.out.println(TCPStreamUtils.g.toJson(TCPSConnectionMetrics));
        }
    }




}
