package org.tcpStreamingAPI.handlerFunctions.receiver;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChannelFuncHandlers {
    private ChannelActiveHandler activeFunction;
    private ChannelActiveReadHandler controlDataHandler;
    private ChannelReadHandler channelReadHandler;
    private ChannelInactiveHandler channelInactiveHandler;
    private OpenConnectionFailedHandler connectionFailedHandler;
}
