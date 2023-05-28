package org.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.connectionSetups.StreamInConnection;
import org.tcpStreamingAPI.connectionSetups.StreamOutConnection;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.utils.TCPStreamUtils;
import org.tcpStreamingAPI.utils.MetricsDisabledException;
import quicSupport.utils.enums.ConnectionOrStreamType;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.QUICLogics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class StreamingChannel implements StreamingNettyConsumer, TCPChannelInterface{
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private final Map<InetSocketAddress, CustomTCPConnection> connections;
    private final Map<String,InetSocketAddress> channelIds;

    private final StreamInConnection server;
    private final StreamOutConnection client;
    private final boolean metricsOn;
    private final TCPStreamMetrics tcpStreamMetrics;
    private final Map<InetSocketAddress,List<byte []>> connecting;
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
        if(singleThreaded){
            connections = new HashMap<>();
            channelIds = new HashMap<>();
            connecting=new HashMap<>();
        }else{
            connections = new ConcurrentHashMap<>();
            channelIds = new ConcurrentHashMap<>();
            connecting=new ConcurrentHashMap<>();
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
    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public  void onChannelInactive(String channelId){
        InetSocketAddress peer = channelIds.remove(channelId);
        if(peer==null){
            return;
        }
        CustomTCPConnection chan = connections.remove(peer);
        if(metricsOn){
            tcpStreamMetrics.onConnectionClosed(chan.channel.remoteAddress());
        }
        channelHandlerMethods.onChannelInactive(peer);

    }

    public void onChannelRead(String channelId, byte[] bytes, ConnectionOrStreamType type){
        InetSocketAddress from = channelIds.get(channelId);
        if(from==null){
            logger.info("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else if(ConnectionOrStreamType.STRUCTURED_MESSAGE==type){
            channelHandlerMethods.onChannelMessageRead(channelId,bytes,from);
        }else{
            channelHandlerMethods.onChannelStreamRead(channelId,bytes,from);
        }
    }

    private void disconnectOldConnection(Channel old){
        channelIds.remove(old.id().asShortText());
        old.disconnect();
    }
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, ConnectionOrStreamType type){
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
            CustomTCPConnection oldConnection = connections.put(listeningAddress,new CustomTCPConnection(channel,type));
            if(oldConnection!=null ){
                int comp = QUICLogics.compAddresses(self,listeningAddress);
                if(comp<0){//2 PEERS SIMULTANEOUSLY CONNECTING TO EACH OTHER
                    //keep the in connection
                    if(inConnection){
                        disconnectOldConnection(oldConnection.channel);
                        logger.info("{} KEEPING NEW CONNECTION {}. IN CONNECTION: {}. FROM {}",self,channel.id().asShortText(), inConnection,listeningAddress);
                    }else{
                        connections.put(listeningAddress,oldConnection);
                        channel.disconnect();
                        logger.info("{} KEEPING OLD CONNECTION {}. IN CONNECTION: {}. FROM {}",self,oldConnection.channel.id().asShortText(),inConnection,listeningAddress);
                        sendPendingMessages(listeningAddress);
                        return;
                    }
                }else if(comp>0) /* if(self.hashCode()>listeningAddress.hashCode()) */ {
                    //keep the out connection
                    if (inConnection) {
                        channel.disconnect();
                        connections.put(listeningAddress,oldConnection);
                        logger.info("{} KEEPING THE OLD CONNECTION {}. IN CONNECTION: {}. FROM {}",self,oldConnection.channel.id().asShortText(), inConnection,listeningAddress);
                        sendPendingMessages(listeningAddress);
                        return;
                    } else {
                        disconnectOldConnection(oldConnection.channel);
                        logger.info("{} KEEPING THE NEW CONNECTION {}. IN CONNECTION: {}. FROM {}",self,channel.id().asShortText(),inConnection,listeningAddress);
                    }
                }
            }else{
                logger.debug("CONNECTION TO {} ACTIVATED.",listeningAddress);
            }
            channelIds.put(channel.id().asShortText(),listeningAddress);
            if(metricsOn){
                tcpStreamMetrics.updateConnectionMetrics(channel.remoteAddress(),listeningAddress,inConnection);
            }
            sendPendingMessages(listeningAddress);
            channelHandlerMethods.onChannelActive(channel,inConnection,listeningAddress,type);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }


    public void onConnectionFailed(String channelId, Throwable cause){
        InetSocketAddress peer = channelIds.get(channelId);
        handleOpenConnectionFailed(peer,cause);
    }

    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/

    public void openConnection(InetSocketAddress peer, ConnectionOrStreamType type) {
        if(connections.containsKey(peer)){
            logger.debug("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            if(connecting.containsKey(peer)){
                return;
            }else{
                connecting.put(peer,new LinkedList<>());
            }
            logger.debug("{} CONNECTING TO {}",self,peer);
            try {
                client.connect(peer,tcpStreamMetrics,this,type);
            }catch (Exception e){
                e.printStackTrace();
                handleOpenConnectionFailed(peer,e.getCause());
            }
        }
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
    private void sendPendingMessages(InetSocketAddress peer){
        List<byte []> messages = connecting.remove(peer);
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self,messages.size(),peer);
            for (byte[] message : messages) {
                send(message,message.length,peer, ConnectionOrStreamType.STRUCTURED_MESSAGE);
            }
        }
    }
    public void closeServerSocket(){
        server.closeServerSocket();
    }
    public void send(byte[] message, int len, InetSocketAddress peer, ConnectionOrStreamType type){
        CustomTCPConnection connection = connections.get(peer);
        if(connection.channel==null){
            List<byte []> pendingMessages = connecting.get(peer);
            if( pendingMessages !=null ){
                pendingMessages.add(message);
                logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            }else if(connectIfNotConnected){
                openConnection(peer, type);
                connecting.get(peer).add(message);
            }else{
                channelHandlerMethods.onMessageSent(message,peer,new Throwable("Unknown Peer : "+peer),type);
            }
        }else if(connection.type==type){
            ByteBuf byteBuf = Unpooled.buffer(message.length+4);
            byteBuf.writeInt(len);
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
                channelHandlerMethods.onMessageSent(message,peer,future.cause(), type);
            });
        }else{
            channelHandlerMethods.onMessageSent(message,peer,new Throwable("CONNECTION DATA TYPE IS "+connection.type+" AND RECEIVED "+type+" DATA TYPE"), type);
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
