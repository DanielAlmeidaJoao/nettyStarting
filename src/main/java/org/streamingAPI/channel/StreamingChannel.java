package org.streamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.client.StreamOutConnection;
import org.streamingAPI.metrics.TCPStreamConnectionMetrics;
import org.streamingAPI.metrics.TCPStreamMetrics;
import org.streamingAPI.server.StreamInConnection;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.utils.FactoryMethods;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StreamingChannel implements StreamingNettyConsumer{
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    private InetSocketAddress self;
    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private Map<InetSocketAddress, Channel> connections;
    private Map<String,InetSocketAddress> channelIds;

    private StreamInConnection server;
    private StreamOutConnection client;
    private final boolean metricsOn;
    private final TCPStreamMetrics tcpStreamMetrics;
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
        }else{
            connections = new ConcurrentHashMap<>();
            channelIds = new ConcurrentHashMap<>();
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
    public  void channelInactive(String channelId){
        InetSocketAddress peer = channelIds.remove(channelId);
        Channel chan = connections.remove(peer);
        if(metricsOn){
            tcpStreamMetrics.onConnectionClosed(chan.remoteAddress());
        }
        System.out.println(FactoryMethods.g.toJson(tcpStreamMetrics.getConnectionMetrics(chan.remoteAddress())));
        onChannelInactive(peer);
    }
    private void test(InetSocketAddress host){
        new Thread(() -> {
            System.out.println("TESTING STARTED!!!");
            while (true){
                Channel channel = connections.get(host);
                if(channel!=null){
                    channelIds.get(channel.id().asShortText()).getPort();
                }
            }
        }).start();
    }
    public abstract void onChannelInactive(InetSocketAddress peer);

    public void channelRead(String channelId, byte[] bytes){
        onChannelRead(channelId,bytes,channelIds.get(channelId));
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    public void channelActive(Channel channel, HandShakeMessage handShakeMessage){
        logger.info("{} CHANNEL ACTIVATED.",self);
        InetAddress hostName;
        int port;
        try {
            boolean incoming;
            InetSocketAddress listeningAddress;
            if(handShakeMessage==null){//out connection
                listeningAddress = (InetSocketAddress) channel.remoteAddress();
                incoming = false;
            }else {//in connection
                listeningAddress = handShakeMessage.getAddress();
                incoming = true;
            }
            connections.put(listeningAddress,channel);
            channelIds.put(channel.id().asShortText(),listeningAddress);
            if(metricsOn){
                tcpStreamMetrics.updateConnectionMetrics(channel.remoteAddress(),listeningAddress,incoming);
            }
            onChannelActive(channel,handShakeMessage,listeningAddress);
            logger.info("CONNECTION TO {} ACTIVATED.",listeningAddress);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }


    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer);

    public void onConnectionFailed(String channelId, Throwable cause){
        InetSocketAddress peer = channelIds.get(channelId);
        onOpenConnectionFailed(peer,cause);
    }

    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/

    protected void openConnection(InetSocketAddress peer) {
        if(connections.containsKey(peer)){
            logger.info("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            logger.info("{} CONNECTING TO {}",self,peer);
            client.connect(peer,true,tcpStreamMetrics,this);
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

    public void send(byte[] message, int len,InetSocketAddress host){
        send(Unpooled.copiedBuffer(message,0,len),host);
    }
    /**
     * ByteBuf buf = ...
     * buf.writeInt(dataLength);
     * but.writeBytes(data)
     * sendDelimited(buf,promise)
     * @param byteBuf
     */
    public void send(ByteBuf byteBuf,InetSocketAddress peer){
        byte [] data = byteBuf.array();
        Channel channel = connections.get(peer);
        if(channel==null){
            sendFailed(peer,new Throwable("Unknown Peer : "+peer));
            return;
        }
        ChannelFuture f =  channel.writeAndFlush(byteBuf);
        f.addListener(future -> {
            if(future.isSuccess()){
                if(metricsOn){
                    TCPStreamConnectionMetrics metrics1 = tcpStreamMetrics.getConnectionMetrics(f.channel().remoteAddress());
                    metrics1.setSentAppBytes(metrics1.getSentAppBytes()+data.length);
                    metrics1.setSentAppMessages(metrics1.getSentAppMessages()+1);
                }
                sendSuccess(data,peer);
            }else {
                sendFailed(peer,future.cause());
            }
        });
    }

    /******************************************* USER EVENTS ****************************************************/


    public abstract void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);
    public abstract void sendFailed(InetSocketAddress peer, Throwable reason);
    public abstract void sendSuccess(byte[] data, InetSocketAddress peer);
    protected void onOutboundConnectionUp() {}


    protected void onOutboundConnectionDown() {
    }



    protected void onInboundConnectionUp() {

    }

    protected void onInboundConnectionDown() {

    }

    public void onServerSocketBind(boolean success, Throwable cause) {
        if (success)
            logger.debug("Server socket ready");
        else
            logger.error("Server socket bind failed: " + cause);
    }

    public void onServerSocketClose(boolean success, Throwable cause) {
        logger.debug("Server socket closed. " + (success ? "" : "Cause: " + cause));
    }

}
