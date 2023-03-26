package quicSupport.handlers.nettyFuncHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.InetSocketAddress;

@AllArgsConstructor
@Getter
public class QuicListenerExecutor {

    private final DefaultEventExecutor loop;
    private final QuicFuncHandlers handlerFunctions;

    public void onChannelActive(QuicStreamChannel defaultStream, byte [] controlData, InetSocketAddress remotePeer){
        loop.execute(() -> {
            handlerFunctions.getConnectionActive().execute(defaultStream,controlData,remotePeer);
        });
    }

    public void onChannelInactive(String channelId){
        loop.execute(() -> {
            handlerFunctions.getConnectionInactive().execute(channelId);
        });
    }

    public void onConnectionError(InetSocketAddress peer, Throwable cause){
        loop.execute(() -> {
            handlerFunctions.getConnectionError().execute(peer,cause);
        });
    }

    public void onStreamCreated(QuicStreamChannel channel){
        loop.execute(() -> {
            handlerFunctions.getStreamCreated().execute(channel);
        });
    }
    public void onStreamClosed(QuicStreamChannel channel){
        loop.execute(() -> {
            handlerFunctions.getStreamClosed().execute(channel);
        });
    }

    public void onStreamError(QuicStreamChannel channel, Throwable cause){
        loop.execute(() -> {
            handlerFunctions.getErrorHandler().execute(channel,cause);
        });
    }

    public void onChannelRead(String streamId, byte[] data) {
        loop.execute(() -> {
            handlerFunctions.getStreamReader().execute(streamId,data);
        });
    }
    public void onKeepAliveMessage(String connectionId){
        loop.execute(() -> {
            handlerFunctions.getOnConnectionKeepAlive().execute(connectionId);
        });
    }
}
