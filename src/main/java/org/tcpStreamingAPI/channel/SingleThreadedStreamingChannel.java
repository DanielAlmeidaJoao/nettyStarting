package org.tcpStreamingAPI.channel;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import org.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import org.tcpStreamingAPI.utils.MetricsDisabledException;
import quicSupport.utils.enums.ConnectionOrStreamType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedStreamingChannel extends StreamingChannel{
    private static final Logger logger = LogManager.getLogger(SingleThreadedStreamingChannel.class);

    private final DefaultEventExecutor executor;
    public SingleThreadedStreamingChannel(Properties properties, TCPChannelHandlerMethods chm, NetworkRole role) throws IOException {
        super(properties,true,chm,role);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, ConnectionOrStreamType type) {
        executor.execute(() -> super.onChannelActive(channel,handShakeMessage, type));
    }

    @Override
    public void onChannelRead(String channelId, byte[] bytes, ConnectionOrStreamType type) {
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

    public void openConnection(InetSocketAddress peer, ConnectionOrStreamType type) {
        executor.execute(() -> super.openConnection(peer, type));
    }
    @Override
    public void closeConnection(InetSocketAddress peer) {
        executor.execute(() -> super.closeConnection(peer));
    }
    @Override
    public void send(byte[] message, int len, InetSocketAddress host, ConnectionOrStreamType structuredMessage){
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
