package org.example.client;

import io.netty.channel.ChannelOption;

public interface StreamSender {
    void connect();

    <T> void updateConfiguration(ChannelOption<T> option, T value);

    void printSomeConfigs();

    void close();

    void sendBytes(byte[] message, int len);

    String streamId();
}
