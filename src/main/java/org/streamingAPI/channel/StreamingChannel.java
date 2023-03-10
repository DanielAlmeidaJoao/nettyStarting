package org.streamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseNotifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.client.StreamOutConnection;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.StreamInConnection;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import org.streamingAPI.server.listeners.InNettyChannelListener;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;

public abstract class StreamingChannel {
    private static final Logger logger = LogManager.getLogger(StreamingChannel.class);
    private InetSocketAddress self;
    public final static String NAME = "STREAMING_CHANNEL";

    public final static String ADDRESS_KEY = "address";
    public final static String PORT_KEY = "port";

    public final static String DEFAULT_PORT = "8574";
    private Map<StreamingHost, Channel> connections;
    private StreamInConnection server;
    private StreamOutConnection client;
    public StreamingChannel( Properties properties)throws Exception{
        InetAddress addr;
        if (properties.containsKey(ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(ADDRESS_KEY));
        else
            throw new IllegalArgumentException(NAME + " requires binding address");

        int port = Integer.parseInt(properties.getProperty(PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        ChannelFuncHandlers handlers = new ChannelFuncHandlers(this::channelActive,this::channelReadConfigData,this::channelRead,this::channelClosed);
        InNettyChannelListener listener = new InNettyChannelListener(StreamInConnection.newDefaultEventExecutor(),handlers);
        server = new StreamInConnection(addr.getHostName(),port,listener);
        client = new StreamOutConnection(listener);

        try{
            server.startListening(false,true);
        }catch (Exception e){
            throw new IOException(e);
        }

    }

    public abstract void channelClosed(String channelId);

    public abstract void channelRead(String channelId, byte[] bytes);

    public abstract void channelReadConfigData(String s, byte[] bytes);

    public void channelActive(Channel channel, HandShakeMessage handShakeMessage){
        InetAddress hostName;
        int port;
        try {
            if(handShakeMessage==null){//out connection
                InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
                hostName = address.getAddress();
                port = address.getPort();
            }else {//in connection
                hostName = InetAddress.getByName(handShakeMessage.getHost());
                port = handShakeMessage.getPort();
            }
            connections.put(new StreamingHost(hostName,port),channel);
            onChannelActive(channel,handShakeMessage);
        }catch (Exception e){
            e.printStackTrace();
            channel.disconnect();
        }
    }
    public abstract void onChannelActive(Channel channel, HandShakeMessage handShakeMessage);



    protected void onOpenConnection(InetSocketAddress peer) {
        client.connect(peer.getAddress().getHostName(),peer.getPort());
    }

    protected void send(byte [] message, InetSocketAddress peer) {
        connections.get(peer).writeAndFlush(message);
    }

    public void send(byte[] message, int len,InetSocketAddress host){
        sendWithListener(message,len, null,host);
    }
    public void sendWithListener(byte[] message, int len, Promise<Void> promise,InetSocketAddress peer){
        sendDelimited(Unpooled.copiedBuffer(message,0,len), promise,peer);
    }
    /**
     * ByteBuf buf = ...
     * buf.writeInt(dataLength);
     * but.writeBytes(data)
     * sendDelimited(buf,promise)
     * @param byteBuf
     * @param promise
     */
    public void sendDelimited(ByteBuf byteBuf, Promise<Void> promise,InetSocketAddress peer){
        ChannelFuture f =  connections.get(peer).writeAndFlush(byteBuf);
        if (promise!=null){
            f.addListener(new PromiseNotifier<>(promise));
        }
    }

    protected void onOutboundConnectionUp() {}

    protected void onCloseConnection(Host peer, int connection) {
        logger.debug("CloseConnection " + peer);
    }

    protected void onOutboundConnectionDown() {
    }

    protected void onOutboundConnectionFailed() {
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
