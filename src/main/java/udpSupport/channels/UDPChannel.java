package udpSupport.channels;

import udpSupport.client_server.NettyUDPServer;
import udpSupport.metrics.ChannelStats;

import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class UDPChannel implements UDPChannelConsumer{

    private NettyUDPServer nettyUDPServer;
    private ChannelStats metrics;
    public UDPChannel(Properties properties, boolean singleThreaded) throws Exception {
        metrics = new ChannelStats();
        nettyUDPServer=new NettyUDPServer(this,metrics);
    }

    public void sendMessage(byte [] message, InetSocketAddress dest){
        nettyUDPServer.sendMessage(message,dest);
    }

    @Override
    public void deliver(byte[] message, InetSocketAddress from) {

    }

    @Override
    public abstract void messageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest);
}
