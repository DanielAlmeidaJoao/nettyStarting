package org.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.connectionSetups.StreamOutConnection;
import org.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import org.tcpStreamingAPI.metrics.TCPStreamConnectionMetrics;
import org.tcpStreamingAPI.metrics.TCPStreamMetrics;
import org.tcpStreamingAPI.connectionSetups.StreamInConnection;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.utils.FactoryMethods;
import org.tcpStreamingAPI.utils.MetricsDisabledException;
import quicSupport.utils.QUICLogics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class StreamingChannel implements StreamingNettyConsumer{
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    private InetSocketAddress self;

    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private final Map<InetSocketAddress, Channel> connections;
    private final Map<String,InetSocketAddress> channelIds;

    private final StreamInConnection server;
    private final StreamOutConnection client;
    private final boolean metricsOn;
    private final TCPStreamMetrics tcpStreamMetrics;
    private final Map<InetSocketAddress,List<byte []>> connecting;


    public StreamingChannel( Properties properties, boolean singleThreaded)throws IOException{
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

        server = new StreamInConnection(addr.getHostName(),port);
        client = new StreamOutConnection(self);

        try{
            server.startListening(false,true,tcpStreamMetrics,this);
        }catch (Exception e){
            throw new IOException(e);
        }
    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public  void onChannelInactive(String channelId){
        InetSocketAddress peer = channelIds.remove(channelId);
        if(peer==null){
            return;
        }

        Channel chan = connections.remove(peer);
        if(metricsOn){
            tcpStreamMetrics.onConnectionClosed(chan.remoteAddress());
        }
        onChannelInactive(peer);

    }
    public abstract void onChannelInactive(InetSocketAddress peer);

    public void onChannelRead(String channelId, byte[] bytes){
        InetSocketAddress from = channelIds.get(channelId);
        if(from==null){
            logger.info("RECEIVED MESSAGE FROM A DISCONNECTED PEER!");
        }else{
            onChannelRead(channelId,bytes,from);
        }
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    private void disconnectOldConnection(Channel old){
        channelIds.remove(old.id().asShortText());
        old.disconnect();
    }
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage){
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
            Channel oldConnection = connections.put(listeningAddress,channel);
            if(oldConnection!=null ){
                int comp = QUICLogics.compAddresses(self,listeningAddress);
                if(comp<0){//2 PEERS SIMULTANEOUSLY CONNECTING TO EACH OTHER
                    //keep the in connection
                    if(inConnection){
                        disconnectOldConnection(oldConnection);
                        logger.info("{} KEEPING NEW CONNECTION {}. IN CONNECTION: {}. FROM {}",self,channel.id().asShortText(), inConnection,listeningAddress);
                    }else{
                        connections.put(listeningAddress,oldConnection);
                        channel.disconnect();
                        logger.info("{} KEEPING OLD CONNECTION {}. IN CONNECTION: {}. FROM {}",self,oldConnection.id().asShortText(),inConnection,listeningAddress);
                        sendPendingMessages(listeningAddress);
                        return;
                    }
                }else if(comp>0) /* if(self.hashCode()>listeningAddress.hashCode()) */ {
                    //keep the out connection
                    if (inConnection) {
                        channel.disconnect();
                        connections.put(listeningAddress,oldConnection);
                        logger.info("{} KEEPING THE OLD CONNECTION {}. IN CONNECTION: {}. FROM {}",self,oldConnection.id().asShortText(), inConnection,listeningAddress);
                        sendPendingMessages(listeningAddress);
                        return;
                    } else {
                        disconnectOldConnection(oldConnection);
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
            onChannelActive(channel,handShakeMessage,listeningAddress);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }

    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer);

    public void onConnectionFailed(String channelId, Throwable cause){
        InetSocketAddress peer = channelIds.get(channelId);
        handleOpenConnectionFailed(peer,cause);
    }

    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/

    protected void openConnection(InetSocketAddress peer) {
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
                client.connect(peer,tcpStreamMetrics,this);
            }catch (Exception e){
                e.printStackTrace();
                handleOpenConnectionFailed(peer,e.getCause());
            }
        }
    }
    protected void closeConnection(InetSocketAddress peer) {
        logger.info("CLOSING CONNECTION TO {}", peer);
        Channel channel = connections.get(peer);
        if(channel!=null){
            channel.close();
        }else {
            logger.info("{} CONNECTION TO {} ALREADY CLOSED",self,peer);
        }
    }
    private void sendPendingMessages(InetSocketAddress peer){
        List<byte []> messages = connecting.remove(peer);
        if(messages!=null){
            logger.debug("{}. THERE ARE {} PENDING MESSAGES TO BE SENT TO {}",self,messages.size(),peer);
            for (byte[] message : messages) {
                send(message,message.length,peer);
            }
        }
    }
    protected void closeServerSocket(){
        server.closeServerSocket();
    }
    public void send(byte[] message, int len,InetSocketAddress peer){
        List<byte []> pendingMessages = connecting.get(peer);
        if( pendingMessages !=null ){
            pendingMessages.add(message);
            logger.debug("{}. MESSAGE TO {} ARCHIVED.",self,peer);
            return;
        }
        Channel channel = connections.get(peer);
        ByteBuf byteBuf = Unpooled.buffer(message.length+4);
        byteBuf.writeInt(len);
        byteBuf.writeBytes(message,0,len);
        if(channel==null){
            sendFailed(peer,new Throwable("Unknown Peer : "+peer));
            return;
        }
        ChannelFuture f =  channel.writeAndFlush(byteBuf);
        f.addListener(future -> {
            if(future.isSuccess()){
                if(metricsOn){
                    TCPStreamConnectionMetrics metrics1 = tcpStreamMetrics.getConnectionMetrics(f.channel().remoteAddress());
                    metrics1.setSentAppBytes(metrics1.getSentAppBytes()+message.length);
                    metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
                }
                sendSuccess(message,peer);
            }else {
                sendFailed(peer,future.cause());
            }
        });
    }

    /******************************************* USER EVENTS ****************************************************/

    public void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause){
        connecting.remove(peer);
        onOpenConnectionFailed(peer,cause);
    }
    public abstract void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    public abstract void sendFailed(InetSocketAddress peer, Throwable reason);
    public abstract void sendSuccess(byte[] data, InetSocketAddress peer);
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
            System.out.println(FactoryMethods.g.toJson(tcpStreamConnectionMetrics));
        }
        System.out.println("old: ");
        ll = tcpStreamMetrics.oldMetrics();
        for (TCPStreamConnectionMetrics tcpStreamConnectionMetrics : ll) {
            System.out.println(FactoryMethods.g.toJson(tcpStreamConnectionMetrics));
        }
    }
}
