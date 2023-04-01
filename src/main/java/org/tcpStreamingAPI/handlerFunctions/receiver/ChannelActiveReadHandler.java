package org.tcpStreamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface ChannelActiveReadHandler {
    public void execute(String channelId,byte [] data);
}
