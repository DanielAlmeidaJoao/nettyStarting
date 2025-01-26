package pt.unl.fct.di.novasys.network.ChannelLogicsWithNetty.NettyQuicChannel.QuicNettyImplementations;

import io.netty.buffer.ByteBuf;

import java.io.InputStream;

public interface SendStreamInterface {

        void sendStream(String customConId , ByteBuf byteBuf, boolean flush);

        void sendInputStream( String conId, InputStream inputStream, long len);

        boolean flushStream(String conId);
    }
