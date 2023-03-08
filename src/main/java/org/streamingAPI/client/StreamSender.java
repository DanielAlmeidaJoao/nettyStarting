package org.streamingAPI.client;

import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;

public interface StreamSender {
    void connect();

    <T> void updateConfiguration(ChannelOption<T> option, T value);

    void close();

    void send(byte[] message, int len);

    void sendWithListener(byte[] message, int len, Promise<Void> promise);

    String streamId();

    void setHost(String hostname, int port);

    DefaultEventExecutor getDefaultEventExecutor();

}
