package tcpSupport.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpStreamingAPI.utils.MetricsDisabledException;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedTCPChannel extends TCPChannel {
    private static final Logger logger = LogManager.getLogger(SingleThreadedTCPChannel.class);

    private final DefaultEventExecutor executor;
    public SingleThreadedTCPChannel(Properties properties, TCPChannelHandlerMethods chm, NetworkRole role) throws IOException {
        super(properties,true,chm,role);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, Pair<InetSocketAddress, String> identification) {
        executor.execute(() -> super.onChannelActive(channel,handShakeMessage, type, identification));
    }

    @Override
    public void onChannelRead(Pair<InetSocketAddress, String> channelId, byte[] bytes, TransmissionType type) {
        executor.execute(() -> super.onChannelRead(channelId,bytes, type));
    }

    @Override
    public void onChannelInactive(Pair<InetSocketAddress, String> channelId) {
        executor.execute(() -> super.onChannelInactive(channelId));
    }

    @Override
    public void onConnectionFailed(Pair<InetSocketAddress, String> channelId, Throwable cause) {
        executor.execute(() -> super.onConnectionFailed(channelId,cause));
    }

    public String openConnection(InetSocketAddress peer, TransmissionType type) {
        final String conId = nextId();
        executor.execute(() -> super.openLogics(peer,type,conId));
        return conId;
    }
    @Override
    public void closeConnection(InetSocketAddress peer) {
        executor.execute(() -> super.closeConnection(peer));
    }
    @Override
    public void send(byte[] message, int len, InetSocketAddress host, TransmissionType structuredMessage){
        executor.execute(() -> super.send(message,len,host, structuredMessage));
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
    public void handleOpenConnectionFailed(Pair<InetSocketAddress, String> peer, Throwable cause){
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
