package org.streamingAPI.server.listeners;


import io.netty.util.concurrent.DefaultEventExecutor;
import org.streamingAPI.handlerFunctions.receiver.ChannelHandlers;

public class InChannelListener {
    private final DefaultEventExecutor loop;
    private final ChannelHandlers handlerFunctions;


    public InChannelListener(DefaultEventExecutor loop, ChannelHandlers handlerFunctions) {
        this.loop = loop;
        this.handlerFunctions = handlerFunctions;
    }

    public void setActiveFunction (String channelId){
        loop.execute(() -> {
            handlerFunctions.getActiveFunction().execute(channelId);
        });
    }
    public void setControlData(String channelId,byte [] data){
        loop.execute(() -> {
            handlerFunctions.getControlDataHandler().execute(channelId,data);
        });
    }

    public void setChannelReadHandler(String channelId, byte [] data){
        loop.execute(() -> {
            handlerFunctions.getChannelReadHandler().execute(channelId,data);
        });
    }

    public void setChannelInactiveHandler(String channelId){
        loop.execute(() -> {
            handlerFunctions.getChannelInactiveHandler().execute(channelId);
        });
    }
}
