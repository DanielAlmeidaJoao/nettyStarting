package org.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.connectionSetups.StreamInConnection;
import org.tcpStreamingAPI.connectionSetups.StreamOutConnection;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.utils.MetricsDisabledException;
import org.tcpStreamingAPI.utils.TCPStreamUtils;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class StreamingChannel implements StreamingNettyConsumer, TCPChannelInterface{
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private final Map<InetSocketAddress, CustomTCPConnection> connections;
    private final Map<InetSocketAddress,Pair<String,List<Pair<byte [],Integer>>>> connecting;

    private final Map<String,CustomTCPConnection> links;


    private final StreamInConnection server;
    private final StreamOutConnection client;
    private final boolean metricsOn;
    private final TCPStreamMetrics tcpStreamMetrics;
    private final boolean connectIfNotConnected;
    private final TCPChannelHandlerMethods channelHandlerMethods;

    private final AtomicInteger idCounterGenerator;


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
        idCounterGenerator = new AtomicInteger();
    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public String nextId(){
        return String.format("tcp_link_",idCounterGenerator.getAndIncrement());
    }
    public  void onChannelInactive(Pair<InetSocketAddress, String> connectionId){
        CustomTCPConnection chan = links.remove(connectionId.getRight());
        if(metricsOn){
            tcpStreamMetrics.onConnectionClosed(chan.channel.remoteAddress());
        }
        channelHandlerMethods.onChannelInactive(connectionId.getKey());

    }

    public void onChannelRead(Pair<InetSocketAddress, String> channelId, byte[] bytes, TransmissionType type){
        if(TransmissionType.STRUCTURED_MESSAGE==type){
            channelHandlerMethods.onChannelMessageRead(channelId.getValue(),bytes,channelId.getKey());
        }else{
            channelHandlerMethods.onChannelStreamRead(channelId.getValue(),bytes,channelId.getKey());
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
            if(!connections.containsKey(listeningAddress)){
                connections.put(listeningAddress,customTCPConnection);
            }
            links.put(identification.getRight(),customTCPConnection);
            if(metricsOn){
                tcpStreamMetrics.updateConnectionMetrics(channel.remoteAddress(),listeningAddress,inConnection);
            }
            sendPendingMessages(listeningAddress,type);
            channelHandlerMethods.onChannelActive(channel,inConnection,listeningAddress,type);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }


    public void onConnectionFailed(Pair<InetSocketAddress, String> connectionId, Throwable cause){
        handleOpenConnectionFailed(connectionId.getKey(),cause);
    }

    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/

    public String openConnection(InetSocketAddress peer, TransmissionType type, String connectionId) {
        if(connectionId == null){
            connectionId = nextId();
        }
        if(connections.containsKey(peer)){
            logger.debug("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            if(connecting.containsKey(peer)){
                return connecting.get(peer).getLeft();
            }else{
                connecting.put(peer,Pair.of(connectionId,new LinkedList<>()));
            }
            logger.debug("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer,tcpStreamMetrics,this,type,connectionId);
            }catch (Exception e){
                e.printStackTrace();
                handleOpenConnectionFailed(peer,e.getCause());
            }
        }
        return connectionId;
    }

    @Override
    public String openConnection(InetSocketAddress peer, TransmissionType type) {
        return openConnection(peer, type,null);
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
    private void sendPendingMessages(InetSocketAddress peer, TransmissionType type){
        List<Pair<byte [],Integer>> messages = connecting.remove(peer).getRight();
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self,messages.size(),peer);
            for (Pair<byte[],Integer> message : messages) {
                send(message.getLeft(),message.getRight(),peer,type);
            }
        }
    }
    public void closeServerSocket(){
        server.closeServerSocket();
    }
    public void send(byte[] message, int len, InetSocketAddress peer, TransmissionType type){
        CustomTCPConnection connection = connections.get(peer);
        if(connection==null){
            List<Pair<byte[],Integer>> pendingMessages = connecting.get(peer).getRight();
            if( pendingMessages !=null ){
                pendingMessages.add(Pair.of(message,len));
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                openConnection(peer, type);
                connecting.get(peer).getRight().add(Pair.of(message,len));
            }else{
                channelHandlerMethods.onMessageSent(message,peer,new Throwable("Unknown Peer : "+peer),type);
            }
        }else{
            send(message,len,connection,type);
        }
    }
    public void send(byte[] message, int len, String connectionId, TransmissionType type){
        CustomTCPConnection connection = links.get(connectionId);
        if(connection==null){
            channelHandlerMethods.onMessageSent(message, null/* peer **/,new Throwable("Unknown Peer : "+null/* peer **/),type);
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
                        TCPStreamConnectionMetrics metrics1 = tcpStreamMetrics.getConnectionMetrics(f.channel().remoteAddress());
                        metrics1.setSentAppBytes(metrics1.getSentAppBytes()+message.length);
                        metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
                    }
                }
                channelHandlerMethods.onMessageSent(message,connection.id.getKey(),future.cause(), type);
            });
        }else{
            channelHandlerMethods.onMessageSent(message,connection.id.getKey(),new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND RECEIVED "+type+" DATA TYPE"), type);
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
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause){
        connecting.remove(peer);
        channelHandlerMethods.onOpenConnectionFailed(peer,cause);
    }

    protected void readMetrics(ReadMetricsHandler handler) throws MetricsDisabledException {
        if(metricsOn){
            handler.readMetrics(tcpStreamMetrics.currentMetrics(),tcpStreamMetrics.oldMetrics());
        }else {
            throw new MetricsDisabledException("METRICS WAS NOT ENABLED!");
        }
    }

    private void debugPrintMetrics(){
        System.out.println("curr: ");
        var ll = tcpStreamMetrics.currentMetrics();
        for (TCPStreamConnectionMetrics tcpStreamConnectionMetrics : ll) {
            System.out.println(TCPStreamUtils.g.toJson(tcpStreamConnectionMetrics));
        }
        System.out.println("old: ");
        ll = tcpStreamMetrics.oldMetrics();
        for (TCPStreamConnectionMetrics tcpStreamConnectionMetrics : ll) {
            System.out.println(TCPStreamUtils.g.toJson(tcpStreamConnectionMetrics));
        }
    }




}
