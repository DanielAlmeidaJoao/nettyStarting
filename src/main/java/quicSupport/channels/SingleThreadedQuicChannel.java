package quicSupport.channels;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import quicSupport.Exceptions.UnknownElement;
import quicSupport.handlers.channelFuncHandlers.OldMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.utils.CustomConnection;
import quicSupport.utils.Logics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class SingleThreadedQuicChannel extends CustomQuicChannel {
    private final DefaultEventExecutor executor;

    public SingleThreadedQuicChannel(Properties properties) throws IOException {
        super(properties);
        executor = new DefaultEventExecutor();
    }

    /*********************************** Stream Handlers **********************************/

    public void streamErrorHandler(QuicStreamChannel channel, Throwable throwable) {
        executor.execute(() -> super.streamErrorHandler(channel,throwable));
    }

    public void streamClosedHandler(QuicStreamChannel channel) {
        executor.execute(() -> super.streamClosedHandler(channel));
    }

    public void streamCreatedHandler(QuicStreamChannel channel) {
        executor.execute(() -> super.streamCreatedHandler(channel));
    }

    public void streamReader(String streamId, byte[] bytes){
        executor.execute(() -> super.streamReader(streamId,bytes));
    }
    public void onKeepAliveMessage(String parentId){
        executor.execute(() -> super.onKeepAliveMessage(parentId));
    }

    /********************************** Stream Handlers **********************************/

    /*********************************** Channel Handlers **********************************/
    public void channelActive(QuicStreamChannel streamChannel, byte [] controlData,InetSocketAddress remotePeer){
        executor.execute(() -> super.channelActive(streamChannel,controlData,remotePeer));
    }
    public  void channelInactive(String channelId){
        executor.execute(() -> super.channelInactive(channelId));
    }

    /*********************************** Channel Handlers **********************************/

    /*********************************** User Actions **************************************/

    public void openConnection(InetSocketAddress peer) {
        executor.execute(() -> super.openConnection(peer));
    }
    public void closeConnection(InetSocketAddress peer){
        executor.execute(() -> super.closeConnection(peer));
    }
    public void getStats(InetSocketAddress peer, QuicConnectionMetricsHandler handler){
        executor.execute(() -> super.getStats(peer,handler));
    }
    public void oldMetrics(OldMetricsHandler handler){
        executor.execute(() -> super.oldMetrics(handler));
    }
    public void createStream(InetSocketAddress peer) {
        executor.execute(() -> super.createStream(peer));
    }
    public void closeStream(String streamId){
        executor.execute(() -> super.closeStream(streamId));
    }

    public void send(String streamId,byte[] message, int len) {
        executor.execute(() -> super.send(streamId,message,len));
    }
    public void send(InetSocketAddress peer,byte[] message, int len) {
        executor.execute(() -> send(peer,message,len));
    }
    /*********************************** User Actions **************************************/

    /*********************************** Other Actions *************************************/
    protected void onOutboundConnectionUp() {
        executor.execute(super::onOutboundConnectionDown);
    }
    protected void onOutboundConnectionDown() {
        executor.execute(super::onOutboundConnectionDown);
    }

    protected void onInboundConnectionUp() {
        executor.execute(super::onInboundConnectionUp);
    }

    protected void onInboundConnectionDown() {
        executor.execute(super::onInboundConnectionDown);
    }

    public void onServerSocketClose(boolean success, Throwable cause) {
        executor.execute(() -> onServerSocketClose(success,cause));
    }
}

