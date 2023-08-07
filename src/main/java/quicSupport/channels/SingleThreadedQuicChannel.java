package quicSupport.channels;

import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import pt.unl.fct.di.novasys.babel.channels.BabelMessageSerializerInterface;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.utils.QuicHandShakeMessage;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.utils.BabelOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedQuicChannel<T> extends NettyQUICChannel<T> {
    private final DefaultEventExecutor executor;

    public SingleThreadedQuicChannel(Properties properties, NetworkRole role, ChannelHandlerMethods mom, BabelMessageSerializerInterface<T> serializer) throws IOException {
        super(properties,true,role,mom,serializer);
        System.out.println("SINGLE THREADED CHANNEL");
        executor = new DefaultEventExecutor();
    }

    /*********************************** Stream Handlers **********************************/

    @Override
    public void streamErrorHandler(QuicStreamChannel channel, Throwable throwable, String customId) {
        executor.submit(() -> super.streamErrorHandler(channel,throwable, customId));
    }

    @Override
    public void streamInactiveHandler(QuicStreamChannel channel, String customId) {
        executor.submit(() -> {
            super.streamInactiveHandler(channel, customId);
        });
    }
    @Override
    public void streamCreatedHandler(QuicStreamChannel channel, TransmissionType type,String customId, boolean inConnection) {
        executor.submit(() ->
        {
            super.streamCreatedHandler(channel, type, customId, inConnection);
        });
    }
    @Override
    public void onReceivedDelimitedMessage(String streamId, ByteBuf bytes){
        executor.submit(() -> {
            super.onReceivedDelimitedMessage(streamId, bytes);
        });
    }
    @Override
    public void onReceivedStream(String streamId, BabelOutputStream bytes) {
        executor.submit(() -> {
            super.onReceivedStream(streamId, bytes);
        });
    }
    @Override
    public void onKeepAliveMessage(String parentId, int i){
        executor.submit(() -> {
            super.onKeepAliveMessage(parentId, i);
        });
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    @Override
    public void channelActive(QuicStreamChannel streamChannel, QuicHandShakeMessage controlData, InetSocketAddress remotePeer, TransmissionType type, int length, String customConId){
        executor.submit(() -> {
            super.channelActive(streamChannel,controlData,remotePeer,type, length, customConId);
        });
    }
    @Override
    public  void channelInactive(String channelId){
        executor.submit(() ->{
            super.channelInactive(channelId);
        });
    }

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/
    public String open(InetSocketAddress peer, TransmissionType type) {
        final String id = nextId();
        executor.submit(() -> {
            super.openLogics(peer,type,id);
        });
        return id;
    }
    @Override
    public void closeConnection(InetSocketAddress peer){
        executor.submit(() -> {
            super.closeConnection(peer);
        });
    }
    @Override
    public void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler){
        executor.submit(() -> {
            super.getStats(peer,handler);
        });
    }

    @Override
    public void closeLink(String streamId){
        executor.submit(() -> {
            super.closeLink(streamId);
        });
    }

    @Override
    public void send(String streamId, T message) {
        executor.submit(() -> {
            super.send(streamId,message);
        });
    }
    @Override
    public void send(InetSocketAddress peer,T message) {
        executor.submit(() -> {
            super.send(peer,message);
        });
    }

    @Override
    public void sendStream(String customConId , ByteBuf byteBuf, boolean flush){
        executor.submit(() -> super.sendStream(customConId,byteBuf,flush));
    }
    public void sendInputStream(String conId, InputStream inputStream, int len)  {
        executor.submit(() -> super.sendInputStream(conId,inputStream,len));
    }

        /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/
        @Override
    public void onServerSocketClose(boolean success, Throwable cause) {
        executor.submit(() -> {
            super.onServerSocketClose(success,cause);
        });
    }
    public void handleOpenConnectionFailed(InetSocketAddress peer, Throwable cause, TransmissionType transmissionType, String id){
        executor.submit(() -> super.handleOpenConnectionFailed(peer,cause, transmissionType, id));
    }
    @Override
    public void shutDown() {
        executor.submit(() -> {
            super.shutDown();
            executor.shutdownGracefully().getNow();
        });
    }
}

