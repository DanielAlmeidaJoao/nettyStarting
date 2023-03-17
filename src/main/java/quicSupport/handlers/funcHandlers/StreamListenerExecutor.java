package quicSupport.handlers.funcHandlers;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;

@AllArgsConstructor
@Getter
public class StreamListenerExecutor {

    private final DefaultEventExecutor loop;
    private final StreamFuncHandlers handlerFunctions;

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
}
