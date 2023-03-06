package org.streamingAPI.client;

import io.netty.channel.ChannelOption;

public interface StreamSender {
    void connect();

    <T> void updateConfiguration(ChannelOption<T> option, T value);

    void close();

    void sendBytes(byte[] message, int len);

    String streamId();
}
