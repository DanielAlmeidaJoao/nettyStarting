package udpSupport.channels;

import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.BabelMessageSerializer;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;
import quicSupport.utils.enums.NetworkRole;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;
import udpSupport.client_server.NettyUDPServer;
import udpSupport.metrics.ChannelStats;
import udpSupport.metrics.UDPNetworkStatsWrapper;
import udpSupport.utils.funcs.OnReadMetricsFunc;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

public class UDPChannel implements UDPChannelConsumer,UDPChannelInterface{
    private static final Logger logger = LogManager.getLogger(UDPChannel.class);
    public final static String NAME = "UDP_CHANNEL";

    public final static String DEFAULT_PORT = "8579";
    private final NettyUDPServer udpServer;
    private ChannelStats metrics;
    private final InetSocketAddress self;
    private final UDPChannelHandlerMethods channelHandlerMethods;
    private final BabelMessageSerializer serializer;

    public UDPChannel(Properties properties, boolean singleThreaded, UDPChannelHandlerMethods consumer, BabelMessageSerializer serializer) throws IOException {
        InetAddress addr;
        if (properties.containsKey(TCPChannelUtils.ADDRESS_KEY))
            addr = Inet4Address.getByName(properties.getProperty(TCPChannelUtils.ADDRESS_KEY));
            //addr = Inet4Address.getByName("0.0.0.0");
        else
            throw new IllegalArgumentException(NAME + " requires binding address");
        this.serializer = serializer;
        int port = Integer.parseInt(properties.getProperty(TCPChannelUtils.PORT_KEY, DEFAULT_PORT));
        self = new InetSocketAddress(addr,port);
        if( properties.getProperty(TCPChannelUtils.CHANNEL_METRICS)!=null){
            metrics = new ChannelStats(singleThreaded);
        }
        udpServer=new NettyUDPServer(this,metrics,self,properties);
        channelHandlerMethods = consumer;
    }

    @Override
    public void shutDownServerClient() {
        udpServer.shutDownServerClient();
    }

    public boolean metricsEnabled(){
        return metrics!=null;
    }
    public void sendMessage(BabelMessage message, InetSocketAddress dest){
        try{
            ByteBuf buf = udpServer.alloc().writeByte(0).writeLong(0);
            serializer.serialize(message,buf);
            udpServer.sendMessage(buf,dest);
            messageSentHandler(true,null,message,dest);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    public InetSocketAddress getSelf(){
        return self;
    }
    public void peerDown(InetSocketAddress peer){
        channelHandlerMethods.onPeerDown(peer);
    }


    @Override
    public void deliverMessage(ByteBuf message, InetSocketAddress from){
        try {
            BabelMessage babelMessage = serializer.deserialize(message);
            channelHandlerMethods.onDeliverMessage(babelMessage,from);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    @Override
    public void messageSentHandler(boolean success, Throwable error, BabelMessage message, InetSocketAddress dest){
        channelHandlerMethods.onMessageSentHandler(success,error,message,dest);
    }

    public void readMetrics(OnReadMetricsFunc onReadMetricsFunc){
        if(metricsEnabled()){
            onReadMetricsFunc.execute(metrics.cloneChannelMetric());
        }else{
            logger.debug("METRICS NOT ENABLED. ADD PROPERTY 'metrics=true'");
        }
    }

    public List<UDPNetworkStatsWrapper> getMetrics(){
        return metrics == null ? null : metrics.cloneChannelMetric();
    }
    @Override
    public NetworkRole getNetworkRole() {
        return NetworkRole.P2P_CHANNEL;
    }


}
