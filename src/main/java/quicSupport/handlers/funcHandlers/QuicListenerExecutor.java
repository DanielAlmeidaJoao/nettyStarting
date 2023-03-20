package quicSupport.handlers.funcHandlers;

import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

import java.net.InetSocketAddress;

@AllArgsConstructor
@Getter
public class QuicListenerExecutor {

    private final DefaultEventExecutor loop;
    private final QuicFuncHandlers handlerFunctions;

    public void onChannelActive(QuicStreamChannel defaultStream, HandShakeMessage handShakeMessage, boolean incoming){
        loop.execute(() -> {
            handlerFunctions.getConnectionActive().execute(defaultStream,handShakeMessage,incoming);
        });
    }

    public void onChannelInactive(String channelId, String streamId){
        loop.execute(() -> {
            handlerFunctions.getConnectionInactive().execute(channelId,streamId);
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
}
