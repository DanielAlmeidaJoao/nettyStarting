/*
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
package quicSupport;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.*;
import io.netty.util.NetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.handlers.server.ServerChannelInitializer;
import quicSupport.handlers.server.ServerInboundConnectionHandler;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public final class QuicServerExample {
    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);

    private QuicServerExample() { }

    public static QuicSslContext getSelfSignedSslContext() throws CertificateException {
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        QuicSslContext sslCtx = QuicSslContextBuilder.forServer(
                        selfSignedCertificate.privateKey(), null, selfSignedCertificate.certificate())
                //.applicationProtocols("http/0.9")
//                .earlyData(true)
                .build();

        return sslCtx;
    }
    public static QuicSslContext getSignedSslContext() throws Exception {
        //SelfSignedCertificate cert = new SelfSignedCertificate();
        //SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        String keystoreFilename = "keystore.jks";
        String keystorePassword = "simple";
        String alias = "quicTestCert";
        //String alias = "wservercert";
        Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);
        //File privateKey = PrivateKeyWriter.writeToFile(pair.getRight(),"key.pem");
        //File cert = CertificateWriter.writeToFile((X509Certificate) pair.getLeft(),"mycert.pem");

        return QuicSslContextBuilder.forServer(
                pair.getRight(), null, (X509Certificate) pair.getLeft())
                .applicationProtocols("tcp")
                .build();
    }
    public static void main(String[] args) throws Exception {


        QuicSslContext context = QuicServerExample.getSignedSslContext();
        //QuicSslContextBuilder.forServer(pair.getRight(), "simple", (X509Certificate) pair.getLeft()).applicationProtocols("quic-echo");
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ChannelHandler codec = new QuicServerCodecBuilder().sslContext(context)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                // Configure some limits for the maximal number of streams (and the data) that we want to handle.
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .initialMaxStreamsUnidirectional(100)

                // Setup a token handler. In a production system you would want to implement and provide your custom
                // one.
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                // ChannelHandler that is added into QuicChannel pipeline.
                .handler(new ServerInboundConnectionHandler())
                .streamHandler(new ServerChannelInitializer()).build();
        try {
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(new InetSocketAddress(NetUtil.LOCALHOST4,8081)).sync().channel();
            System.out.println("SERVER STARTED 2");
            channel.closeFuture().sync();
            System.out.println("SERVER STARTED ");
        } finally {
            group.shutdownGracefully();
        }
    }
}