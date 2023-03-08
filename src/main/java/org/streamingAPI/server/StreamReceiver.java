package org.streamingAPI.server;

import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;

public interface StreamReceiver  {
    void startListening(boolean sync) throws Exception;

    void send(String streamId , byte[] message, int len);

    void sendBytesWithListener(String streamId, byte[] message, int len, Promise<Void> promise);

    <T> void updateConfiguration(ChannelOption<T> option, T value);
    <T> void updateConfiguration(String streamId,ChannelOption<T> option, T value);
    void closeStream(String streamId);
    void close();

    DefaultEventExecutor getDefaultEventExecutor();

}
