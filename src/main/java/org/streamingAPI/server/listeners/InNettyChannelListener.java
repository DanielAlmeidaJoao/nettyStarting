package org.streamingAPI.server.listeners;


import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import lombok.Getter;
import org.streamingAPI.channel.StreamingHost;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

public class InNettyChannelListener {

    @Getter
    private final DefaultEventExecutor loop;
    private final ChannelFuncHandlers handlerFunctions;


    public InNettyChannelListener(DefaultEventExecutor loop, ChannelFuncHandlers handlerFunctions) {
        this.loop = loop;
        this.handlerFunctions = handlerFunctions;
    }

    public void onChannelActive(Channel channelId, HandShakeMessage handShakeMessage){
        loop.execute(() -> {
            handlerFunctions.getActiveFunction().execute(channelId,handShakeMessage);
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
