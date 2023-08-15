package tcpSupport.tcpChannelAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import quicSupport.channels.ChannelHandlerMethods;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.connectionSetups.messages.HandShakeMessage;
import tcpSupport.tcpChannelAPI.handlerFunctions.ReadMetricsHandler;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedNettyTCPChannel<T> extends NettyTCPChannel<T> {
    private static final Logger logger = LogManager.getLogger(SingleThreadedNettyTCPChannel.class);

    private final DefaultEventExecutor executor;
    public SingleThreadedNettyTCPChannel(Properties properties, ChannelHandlerMethods chm, NetworkRole role,BabelMessageSerializerInterface<T> serializer) throws IOException {
        super(properties,true,chm,role,serializer);
        executor = new DefaultEventExecutor();
    }
    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, TransmissionType type, int len) {
        executor.execute(() -> super.onChannelActive(channel,handShakeMessage, type, len));
    }

    @Override
    public void onChannelMessageRead(String channelId, ByteBuf bytes) {
        final ByteBuf copy = bytes.retainedDuplicate();
        executor.execute(() -> {
            super.onChannelMessageRead(channelId,copy);
            copy.release();
        });
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
    public void send(InetSocketAddress host, T message){
        executor.execute(() -> super.send(host,message));
    }
    @Override
    public void send(String conId,T message){
        executor.execute(() -> super.send(conId,message));
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
            super.readMetrics(handler);
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
