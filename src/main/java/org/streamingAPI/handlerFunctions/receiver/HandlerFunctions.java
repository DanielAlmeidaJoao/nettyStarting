package org.streamingAPI.handlerFunctions.receiver;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HandlerFunctions {
    private ChannelActiveHandler activeFunction;
    private ChannelReadHandler functionToExecute;
    private ChannelInactiveHandler EOSFunction;
    private ChannelActiveReadHandler controlData;
}
