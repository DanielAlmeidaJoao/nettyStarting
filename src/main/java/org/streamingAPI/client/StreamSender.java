package org.streamingAPI.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Promise;

public interface StreamSender {
    void connect(String host, int port);

    <T> void updateConfiguration(ChannelOption<T> option, T value);

    void close();

    void send(byte[] message, int len);

    void sendWithListener(byte[] message, int len, Promise<Void> promise);

    /**
     * ByteBuf buf = ...
     * buf.writeInt(dataLength);
     * but.writeBytes(data)
     * sendDelimited(buf,promise)
     * @param byteBuf
     * @param promise
     */
    void sendDelimited(ByteBuf byteBuf, Promise<Void> promise);

    String streamId();

    void setHost(String hostname, int port);

    DefaultEventExecutor getDefaultEventExecutor();

}
