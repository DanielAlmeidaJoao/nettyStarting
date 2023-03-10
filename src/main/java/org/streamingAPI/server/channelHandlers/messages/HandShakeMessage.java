package org.streamingAPI.server.channelHandlers.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class HandShakeMessage {
    private String host;
    private int port;
}
