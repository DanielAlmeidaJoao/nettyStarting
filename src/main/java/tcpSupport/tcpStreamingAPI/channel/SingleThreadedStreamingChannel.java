package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.ChannelHandlerMethods;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpStreamingAPI.utils.MetricsDisabledException;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedStreamingChannel extends StreamingChannel{
    private static final Logger logger = LogManager.getLogger(SingleThreadedStreamingChannel.class);

    private final DefaultEventExecutor executor;
    public SingleThreadedStreamingChannel(Properties properties, ChannelHandlerMethods chm, NetworkRole role) throws IOException {
        super(properties,true,chm,role);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type) {
        executor.execute(() -> super.onChannelActive(channel,handShakeMessage, type));
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes, TransmissionType type) {
        executor.execute(() -> super.onChannelRead(channelId,bytes, type));
    }

    @Override
    public void onChannelInactive(String channelId) {
        executor.execute(() -> super.onChannelInactive(channelId));
    }

    @Override
    public void onConnectionFailed(String channelId, Throwable cause) {
        executor.execute(() -> super.onConnectionFailed(channelId,cause));
    }

    public String open(InetSocketAddress peer, TransmissionType type) {
        final String conId = nextId();
        executor.execute(() -> super.openConnectionLogics(peer, type,conId));
        return conId;
    }
    @Override
    public void closeConnection(InetSocketAddress peer) {
        executor.execute(() -> super.closeConnection(peer));
    }
    @Override
    public void closeLink(String connectionId) {
        executor.execute(() -> super.closeLink(connectionId));
    }
    @Override
    public void send(InetSocketAddress host, byte[] message, int len,TransmissionType transmissionType){
        executor.execute(() -> super.send(host,message,len, transmissionType));
    }
    @Override
    public void send(String conId,byte[] message, int len, TransmissionType transmissionType){
        executor.execute(() -> super.send(conId,message,len, transmissionType));
    }

    @Override
    public void sendInputStream(InputStream inputStream, int len, InetSocketAddress peer, String conId){
        executor.execute(() -> {
            super.sendInputStream(inputStream,len,peer,conId);
        });
    }
    @Override
    public void closeServerSocket(){
        executor.execute(() -> super.closeServerSocket());
    }
    @Override
    public void readMetrics(ReadMetricsHandler handler){
        executor.execute(() -> {
            try {
                super.readMetrics(handler);
            } catch (MetricsDisabledException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause){
        executor.submit(() -> super.handleOpenConnectionFailed(peer,cause));
    }

    @Override
    public void shutDown() {
        executor.submit(() -> {
            super.shutDown();
            executor.shutdownGracefully().getNow();
        });
    }
}