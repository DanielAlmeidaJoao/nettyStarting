package org.streamingAPI.handlerFunctions;


import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.Getter;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.connectionSetups.messages.HandShakeMessage;

public class InNettyChannelListener {
    @Getter
    private final DefaultEventExecutor loop;
    private final ChannelFuncHandlers handlerFunctions;


    public InNettyChannelListener(DefaultEventExecutor loop, ChannelFuncHandlers handlerFunctions) {
        this.loop = loop;
        this.handlerFunctions = handlerFunctions;
    }

    public void onChannelActive(Channel defaultStream, HandShakeMessage handShakeMessage){
        loop.execute(() -> {
            handlerFunctions.getActiveFunction().execute(defaultStream,handShakeMessage);
        });
    }

    public void onControlDataRead(String channelId, byte [] data){
        loop.execute(() -> {
            handlerFunctions.getControlDataHandler().execute(channelId,data);
        });
    }

    public void onChannelRead(String channelId, byte [] data){
        loop.execute(() -> {
            handlerFunctions.getChannelReadHandler().execute(channelId,data);
        });
    }

    public void onChannelInactive(String channelId){
        loop.execute(() -> {
            handlerFunctions.getChannelInactiveHandler().execute(channelId);
        });
    }

    public void onOpenConnectionFailedHandler(String channelId, Throwable cause){
        loop.execute(() -> {
            handlerFunctions.getConnectionFailedHandler().execute(channelId,cause);
        });
    }
}
