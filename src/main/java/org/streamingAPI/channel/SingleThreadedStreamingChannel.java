package org.streamingAPI.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class SingleThreadedStreamingChannel extends StreamingChannel{
    private static final Logger logger = LogManager.getLogger(SingleThreadedStreamingChannel.class);

    private DefaultEventExecutor executor;
    public SingleThreadedStreamingChannel(Properties properties) throws IOException {
        super(properties,true);
        executor = new DefaultEventExecutor();
    }

    @Override
    public void channelActive(Channel channel, HandShakeMessage handShakeMessage) {
        executor.execute(() -> {
            super.channelActive(channel,handShakeMessage);
        });
    }

    @Override
    public void channelRead(String channelId, byte[] bytes) {
        executor.execute(() -> {
            super.channelRead(channelId,bytes);
        });
    }

    @Override
    public void channelInactive(String channelId) {
        executor.execute(() -> {
            super.channelInactive(channelId);
        });
    }

    @Override
    public void onConnectionFailed(String channelId, Throwable cause) {
        executor.execute(() -> {
            super.onConnectionFailed(channelId,cause);
        });
    }

    @Override
    protected void openConnection(InetSocketAddress peer) {
        executor.execute(() -> {
            super.openConnection(peer);
        });
    }
    @Override
    protected void closeConnection(InetSocketAddress peer) {
        executor.execute(() -> {
            super.closeConnection(peer);
        });
    }

    @Override
    public void send(byte[] message, int len,InetSocketAddress host){
        executor.execute(() -> {
            super.send(message,len,host);
        });
    }

    @Override
    public void send(ByteBuf byteBuf, InetSocketAddress peer){
        executor.execute(() -> {
            super.send(byteBuf,peer);
        });
    }
}
