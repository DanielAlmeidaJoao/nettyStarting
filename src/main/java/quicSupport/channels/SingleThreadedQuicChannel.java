package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.ConnectionOrStreamType;
import quicSupport.utils.enums.NetworkRole;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class SingleThreadedQuicChannel extends CustomQuicChannel {
    private DefaultEventExecutor executor;

    public SingleThreadedQuicChannel(Properties properties, NetworkRole role, ChannelHandlerMethods mom) throws IOException {
        super(properties,true,role,mom);
        System.out.println("SINGLE THREADED CHANNEL");
        executor = new DefaultEventExecutor();
        executor.terminationFuture().addListener(future -> {
           System.out.println("TERMINATED WHY ??? "+future.isSuccess()+" cause: "+future.cause());
        });
    }

    /*********************************** Stream Handlers **********************************/

    @Override
    public void streamErrorHandler(QuicStreamChannel channel, Throwable throwable) {
        executor.submit(() -> {
            super.streamErrorHandler(channel,throwable);
        });
    }

    @Override
    public void streamClosedHandler(QuicStreamChannel channel) {
        executor.submit(() -> {
            super.streamClosedHandler(channel);
        });
    }
    @Override
    public void streamCreatedHandler(QuicStreamChannel channel) {
        executor.submit(() ->
        {
            super.streamCreatedHandler(channel);
        });
    }
    @Override
    public void onReceivedDelimitedMessage(String streamId, byte[] bytes){
        executor.submit(() -> {
            super.onReceivedDelimitedMessage(streamId, bytes);
        });
    }
    @Override
    public void onReceivedStream(String streamId, byte [] bytes) {
        executor.submit(() -> {
            super.onReceivedStream(streamId, bytes);
        });
    }
    @Override
    public void onKeepAliveMessage(String parentId){
        executor.submit(() -> {
            super.onKeepAliveMessage(parentId);
        });
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    @Override
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData,InetSocketAddress remotePeer,ConnectionOrStreamType type){
        executor.submit(() -> {
            super.channelActive(streamChannel,controlData,remotePeer,type);
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
    public void open(InetSocketAddress peer, ConnectionOrStreamType type) {
        executor.submit(() -> {
            super.open(peer,type);
        });
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
    public void createStream(InetSocketAddress peer) {
        executor.submit(() -> {
            super.createStream(peer);
        });
    }
    @Override
    public void closeStream(String streamId){
        executor.submit(() -> {
            super.closeStream(streamId);
        });
    }
    @Override
    public void readMetrics(QuicReadMetricsHandler handler){
        executor.submit(() -> {
            super.readMetrics(handler);
        });
    }
    @Override
    public void send(String streamId, byte[] message, int len,ConnectionOrStreamType type) {
        executor.submit(() -> {
            super.send(streamId,message,len,type);
        });
    }
    @Override
    public void send(InetSocketAddress peer, byte[] message, int len, ConnectionOrStreamType type) {
        executor.submit(() -> {
            super.send(peer,message,len,type);
        });
    }
    /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/
        @Override
    public void onServerSocketClose(boolean success, Throwable cause) {
        executor.submit(() -> {
            super.onServerSocketClose(success,cause);
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

