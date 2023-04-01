package org.tcpStreamingAPI.handlerFunctions.receiver;

@FunctionalInterface
public interface ChannelReadHandler {
    public void execute(String id, byte [] data);
}
