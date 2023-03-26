package org.streamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.client.StreamOutConnection;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.StreamInConnection;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.handlerFunctions.InNettyChannelListener;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public abstract class StreamingChannel {
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    @Getter
    private DefaultEventExecutor executor;
    private InetSocketAddress self;
    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private Map<InetSocketAddress, Channel> connections;
    private Map<String,InetSocketAddress> channelIds;

    private StreamInConnection server;
    private StreamOutConnection client;
    public StreamingChannel( Properties properties)throws IOException{
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        ChannelFuncHandlers handlers = new ChannelFuncHandlers(this::channelActive,
                this::channelReadConfigData,
                this::channelRead,
                this::channelInactive,
                this::onConnectionFailed);
        InNettyChannelListener listener = new InNettyChannelListener(StreamInConnection.newDefaultEventExecutor(),handlers);
        connections = new HashMap<>();
        channelIds = new HashMap<>();
        server = new StreamInConnection(addr.getHostName(),port,listener);
        client = new StreamOutConnection(listener,self);
        executor = listener.getLoop();

        try{
            server.startListening(false,true);
        }catch (Exception e){
            throw new IOException(e);
        }

    }

    /******************************************* CHANNEL EVENTS ****************************************************/
    public  void channelInactive(String channelId){
        InetSocketAddress peer = channelIds.remove(channelId);
        connections.remove(peer);
        onChannelInactive(peer);
    }

    public abstract void onChannelInactive(InetSocketAddress peer);

    public void channelRead(String channelId, byte[] bytes){
        onChannelRead(channelId,bytes,channelIds.get(channelId));
    }
    public abstract void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from);

    public abstract void channelReadConfigData(String s, byte[] bytes);

    public void channelActive(Channel channel, HandShakeMessage handShakeMessage){
        logger.info("{} CHANNEL ACTIVATED.",self);
        InetAddress hostName;
        int port;
        try {
            if(handShakeMessage==null){//out connection
                InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
                hostName = address.getAddress();
                port = address.getPort();
            }else {//in connection
                hostName = InetAddress.getByName(handShakeMessage.getHostName());
                port = handShakeMessage.getPort();
            }
            InetSocketAddress listeningAddress = new InetSocketAddress(hostName,port);
            connections.put(listeningAddress,channel);
            channelIds.put(channel.id().asShortText(),listeningAddress);
            onChannelActive(channel,handShakeMessage,listeningAddress);
            logger.info("CONNECTION TO {} ACTIVATED.",listeningAddress);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }
    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage,InetSocketAddress peer);

    /******************************************* CHANNEL EVENTS ****************************************************/

    /******************************************* USER EVENTS ****************************************************/

    protected void openConnection(InetSocketAddress peer) {
        if(connections.containsKey(peer)){
            logger.info("{} ALREADY CONNECTED TO {}",self,peer);
        }else {
            logger.info("{} CONNECTING TO {}",self,peer);
            client.connect(peer,true);
        }
    }
    protected void closeConnection(InetSocketAddress peer) {
        logger.info("CLOSING CONNECTION TO {}", peer);
        connections.get(peer).close();
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
        ChannelFuture f =  connections.get(peer).writeAndFlush(byteBuf);
        f.addListener(future -> {
            if(future.isSuccess()){
                sendSuccess(data,peer);
            }else {
                sendFailed(peer,future.cause());
            }
        });
    }
    public abstract void sendFailed(InetSocketAddress peer, Throwable reason);
    public abstract void sendSuccess(byte[] data, InetSocketAddress peer);
    /******************************************* USER EVENTS ****************************************************/

    public void onConnectionFailed(String channelId, Throwable cause){
        InetSocketAddress peer = channelIds.get(channelId);
        onOpenConnectionFailed(peer,cause);
    }
    public abstract void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause);

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
