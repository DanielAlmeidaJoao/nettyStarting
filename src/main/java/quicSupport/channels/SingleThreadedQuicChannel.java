package quicSupport.channels;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicReadMetricsHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class SingleThreadedQuicChannel extends CustomQuicChannel {
    private final DefaultEventExecutor executor;

    public SingleThreadedQuicChannel(Properties properties) throws IOException {
        super(properties,true);
        executor = new DefaultEventExecutor();
    }

    /*********************************** Stream Handlers **********************************/

    @Override
    public void streamErrorHandler(QuicStreamChannel channel, Throwable throwable) {
        executor.execute(() -> super.streamErrorHandler(channel,throwable));
    }

    @Override
    public void streamClosedHandler(QuicStreamChannel channel) {
        executor.execute(() -> super.streamClosedHandler(channel));
    }
    @Override
    public void streamCreatedHandler(QuicStreamChannel channel) {
        executor.execute(() -> super.streamCreatedHandler(channel));
    }
    @Override
    public void streamReader(String streamId, byte[] bytes){
        executor.execute(() -> super.streamReader(streamId,bytes));
    }
    @Override
    public void onKeepAliveMessage(String parentId){
        executor.execute(() -> super.onKeepAliveMessage(parentId));
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    @Override
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData,InetSocketAddress remotePeer){
        executor.execute(() -> super.channelActive(streamChannel,controlData,remotePeer));
    }
    @Override
    public  void channelInactive(String channelId){
        executor.execute(() -> super.channelInactive(channelId));
    }

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/
    @Override
    public void openConnection(InetSocketAddress peer) {
        executor.execute(() -> super.openConnection(peer));
    }
    @Override
    public void closeConnection(InetSocketAddress peer){
        executor.execute(() -> super.closeConnection(peer));
    }
    @Override
    public void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler){
        executor.execute(() -> super.getStats(peer,handler));
    }
    @Override
    public void createStream(InetSocketAddress peer) {
        executor.execute(() -> super.createStream(peer));
    }
    @Override
    public void closeStream(String streamId){
        executor.execute(() -> super.closeStream(streamId));
    }
    @Override
    public void readMetrics(QuicReadMetricsHandler handler){
        executor.execute(() -> super.readMetrics(handler));
    }
    @Override
    public void send(String streamId,byte[] message, int len) {
        executor.execute(() -> super.send(streamId,message,len));
    }
    @Override
    public void send(InetSocketAddress peer,byte[] message, int len) {
        executor.execute(() -> super.send(peer,message,len));
    }
    /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/
    @Override
    protected void onOutboundConnectionUp() {
        executor.execute(super::onOutboundConnectionDown);
    }
    @Override
    protected void onOutboundConnectionDown() {
        executor.execute(super::onOutboundConnectionDown);
    }
    @Override
    protected void onInboundConnectionUp() {
        executor.execute(super::onInboundConnectionUp);
    }
    @Override
    protected void onInboundConnectionDown() {
        executor.execute(super::onInboundConnectionDown);
    }
    @Override
    public void onServerSocketClose(boolean success, Throwable cause) {
        executor.execute(() -> onServerSocketClose(success,cause));
    }
}

