package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.commons.lang3.tuple.Triple;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;
import quicSupport.utils.enums.NetworkRole;
import quicSupport.utils.enums.TransmissionType;

import java.io.IOException;
import java.io.InputStream;
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
    public void streamInactiveHandler(QuicStreamChannel channel) {
        executor.submit(() -> {
            super.streamInactiveHandler(channel);
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
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData, InetSocketAddress remotePeer, TransmissionType type){
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
    public String createStream(InetSocketAddress peer, TransmissionType type, Triple<Short,Short,Short> args) {
        final String streamId = nextId();
        executor.submit(() -> {
            super.createStreamLogics(peer,type,args,streamId);
        });
        return streamId;
    }
    @Override
    public void closeLink(String streamId){
        executor.submit(() -> {
            super.closeLink(streamId);
        });
    }
    @Override
    public void readMetrics(QuicReadMetricsHandler handler){
        executor.submit(() -> {
            super.readMetrics(handler);
        });
    }
    @Override
    public boolean send(String streamId, byte[] message, int len, TransmissionType type) {
        boolean result = super.containConnection(streamId);
        executor.submit(() -> {
            super.send(streamId,message,len,type);
        });
        return result;
    }
    @Override
    public boolean send(InetSocketAddress peer, byte[] message, int len, TransmissionType type) {
        boolean result = super.containConnection(peer);
        executor.submit(() -> {
            super.send(peer,message,len,type);
        });
        return result;
    }
    @Override
    public boolean sendInputStream(InputStream inputStream, int len, InetSocketAddress peer, String conId){
        boolean res = containConnection(conId) || containConnection(peer);
        executor.submit(() -> {
            super.sendInputStream(inputStream,len,peer,conId);
        });
        return res;
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

