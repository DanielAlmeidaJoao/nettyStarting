package quicSupport;/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicClientCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamType;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.handlers.client.QuicChannelConHandler;
import quicSupport.handlers.client.QuicStreamReadHandler;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public final class QuicClientExample {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    private QuicClientExample() { }

    public static ChannelHandler getCodec()throws Exception{
        String keystoreFilename = "keystore2.jks";
        String keystorePassword = "simple";
        String alias = "clientcert";
        Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);

        QuicSslContext context = QuicSslContextBuilder.forClient().
                //keyManager(pair.getRight(),null, (X509Certificate) pair.getLeft())
                trustManager(InsecureTrustManagerFactory.INSTANCE)
                //trustManager((X509Certificate) pair.getLeft())
                .applicationProtocols("tcp")
                .build();
        ChannelHandler codec = new QuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                // As we don't want to support remote initiated streams just setup the limit for local initiated
                // streams in this example.
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .build();

        return codec;
    }

    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(QuicClientExample.getCodec())
                    .bind(0).sync().channel();

            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                    .streamHandler(new QuicChannelConHandler())
                    .remoteAddress(new InetSocketAddress(NetUtil.LOCALHOST4, 8081))
                    .connect()
                    .get();

            QuicStreamChannel streamChannel = quicChannel
                    .createStream(QuicStreamType.BIDIRECTIONAL,new QuicStreamReadHandler())
                    .sync()
                    .getNow();

            streamChannel.writeAndFlush(Unpooled.copiedBuffer("blablaaba sasa", CharsetUtil.US_ASCII))
                    .addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);

            // Wait for the stream channel and quic channel to be closed (this will happen after we received the FIN).
            // After this is done we will close the underlying datagram channel.
            streamChannel.closeFuture().sync();
            quicChannel.closeFuture().sync();
            channel.close().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}