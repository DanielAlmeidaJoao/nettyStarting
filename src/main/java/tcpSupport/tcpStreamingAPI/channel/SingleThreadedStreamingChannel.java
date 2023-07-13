package tcpSupport.tcpStreamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpStreamingAPI.utils.BabelOutputStream;
import tcpSupport.tcpStreamingAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpStreamingAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpStreamingAPI.utils.MetricsDisabledException;

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
    public void onChannelMessageRead(String channelId, byte[] bytes) {
        executor.execute(() -> super.onChannelMessageRead(channelId,bytes));
    }

    @Override
    public void onChannelStreamRead(String channelId, BabelOutputStream babelOutputStream) {
        executor.execute(() -> super.onChannelStreamRead(channelId, babelOutputStream));
    }

    @Override
    public void onChannelInactive(String channelId) {
        executor.execute(() -> super.onChannelInactive(channelId));
    }

    @Override
    public void onConnectionFailed(String channelId, Throwable cause, TransmissionType type) {
        executor.execute(() -> super.onConnectionFailed(channelId,cause, type));
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
    public void send(InetSocketAddress host, byte[] message, int len){
        executor.execute(() -> super.send(host,message,len));
    }
    @Override
    public void send(String conId,byte[] message, int len){
        executor.execute(() -> super.send(conId,message,len));
    }
    @Override
    public void sendStream(String customConId , ByteBuf byteBuf, boolean flush){
        executor.submit(() -> super.sendStream(customConId,byteBuf,flush));
    }
    public void sendInputStream(String conId, InputStream inputStream, int len)  {
        executor.submit(() -> super.sendInputStream(conId,inputStream,len));
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
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType type, String conId){
        executor.submit(() -> super.handleOpenConnectionFailed(peer,cause, type, conId));
    }

    @Override
    public void shutDown() {
        executor.submit(() -> {
            super.shutDown();
            executor.shutdownGracefully().getNow();
        });
    }
}
