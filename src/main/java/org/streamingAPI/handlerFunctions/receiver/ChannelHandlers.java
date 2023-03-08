package org.streamingAPI.handlerFunctions.receiver;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChannelHandlers {
    private ChannelActiveHandler activeFunction;
    private ChannelActiveReadHandler controlDataHandler;
    private ChannelReadHandler channelReadHandler;
    private ChannelInactiveHandler channelInactiveHandler;

}
