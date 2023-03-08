package org.streamingAPI.server;

import io.netty.channel.ChannelOption;

public interface StreamReceiver {
    void startListening(boolean sync) throws Exception;

    void sendBytes(String streamId ,byte[] message, int len);

    <T> void updateConfiguration(ChannelOption<T> option, T value);
    <T> void updateConfiguration(String streamId,ChannelOption<T> option, T value);
    void closeStream(String streamId);
    void close();

}
