package org.streamingAPI.handlerFunctions.receiver;

import babel.appExamples.channels.messages.StreamMessage;
import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface ChannelReadByteBufHandler {
    public void execute(String id, StreamMessage streamMessage);
}
