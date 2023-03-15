package org.streamingAPI.handlerFunctions;

import io.netty.util.concurrent.DefaultEventExecutor;
import org.streamingAPI.handlerFunctions.receiver.ChannelFuncHandlers;

public class InNettyStreamListener extends InNettyChannelListener{

    public InNettyStreamListener(DefaultEventExecutor loop, ChannelFuncHandlers handlerFunctions) {
        super(loop,handlerFunctions);
    }
}
