package org.streamingAPI.server.listeners;


import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.Getter;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;

public class InChannelListener {

    @Getter
    private final DefaultEventExecutor loop;
    private final ChannelFuncHandlers handlerFunctions;


    public InChannelListener(DefaultEventExecutor loop, ChannelFuncHandlers handlerFunctions) {
        this.loop = loop;
        this.handlerFunctions = handlerFunctions;
    }

    public void onChannelActive(String channelId){
        loop.execute(() -> {
            handlerFunctions.getActiveFunction().execute(channelId);
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
}
