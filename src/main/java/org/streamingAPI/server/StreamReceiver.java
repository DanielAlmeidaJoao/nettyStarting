package org.streamingAPI.server;

import io.netty.channel.ChannelOption;

public interface StreamReceiver {
    void startListening() throws Exception;

    <T> void updateConfiguration(ChannelOption<T> option, T value);

    void closeStream(String streamId);

    void close();

}
