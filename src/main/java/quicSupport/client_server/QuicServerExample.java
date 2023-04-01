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
package quicSupport.client_server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.handlers.channelFuncHandlers.SocketBindHandler;
import quicSupport.handlers.pipeline.QuicServerChannelConHandler;
import quicSupport.utils.LoadCertificate;
import quicSupport.handlers.pipeline.ServerChannelInitializer;
import quicSupport.utils.Logics;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

public final class QuicServerExample {
    private final CustomQuicChannelConsumer consumer;
    private boolean started;
    private final InetSocketAddress self;
    private final Properties properties;
    private QuicChannelMetrics metrics;
    private static final Logger logger = LogManager.getLogger(QuicServerExample.class);


    public QuicServerExample(String host, int port, CustomQuicChannelConsumer consumer, QuicChannelMetrics metrics, Properties properties) {
        this.consumer = consumer;
        self = new InetSocketAddress(host,port);
        this.metrics = metrics;
        started = false;
        this.properties=properties;
    }

    public QuicSslContext getSignedSslContext() throws Exception {
        String keystoreFilename = "keystore.jks";
        String keystorePassword = "simple";
        String alias = "quicTestCert";
        //String alias = "wservercert";
        Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);
        return QuicSslContextBuilder.forServer(
                pair.getRight(), null, (X509Certificate) pair.getLeft())
                .applicationProtocols("QUIC")
                .build();
    }

    public ChannelHandler getChannelHandler(QuicSslContext context, Properties properties) {
        QuicServerCodecBuilder serverCodecBuilder =  new QuicServerCodecBuilder()
                .sslContext(context);
        serverCodecBuilder = (QuicServerCodecBuilder) Logics.addConfigs(serverCodecBuilder,properties);
        ChannelHandler codec = serverCodecBuilder
                // Setup a token handler. In a production system you would want to implement and provide your custom
                // one.
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                // ChannelHandler that is added into QuicChannel pipeline.
                .handler(new QuicServerChannelConHandler(consumer,metrics))
                .streamHandler(new ServerChannelInitializer(consumer,metrics,Logics.INCOMING_CONNECTION))
                .build();
        return codec;
    }

    public void start(SocketBindHandler handler) throws Exception {
        if(started){
            logger.info("SERVER STARTED AT host:{} port:{} ",self.getHostName(),self.getPort());
            handler.execute(true,null);
            return;
        }
        QuicSslContext context = getSignedSslContext();
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelHandler codec = getChannelHandler(context,properties);
        try {
            Bootstrap bs = new Bootstrap();
            bs = bs.option(ChannelOption.SO_RCVBUF,1024*1024)
                    .option(ChannelOption.SO_SNDBUF,1024*1024);

            Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(QuicChannelOption.SO_RCVBUF,1024*1024)
                    .option(QuicChannelOption.SO_SNDBUF,1024*1024)
                    .option(ChannelOption.WRITE_BUFFER_WATER_MARK,new WriteBufferWaterMark(2*1024*1024,2*1024*1024*2))
                    .handler(codec)
                    .bind(self).sync()
                    .addListener(future -> {
                        handler.execute(future.isSuccess(),future.cause());
                    })
                    .channel();
            started=true;

            var config = channel.config();
            config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(1024*1024));

            System.out.println("getWriteBufferLowWaterMark "+config.getWriteBufferLowWaterMark());
            System.out.println(config.getWriteBufferHighWaterMark());
            System.out.println(config.getWriteBufferHighWaterMark());
            System.out.println("SET "+config.setOption(QuicChannelOption.SO_RCVBUF,1024*1024));
            System.out.println(config.getOptions().put(QuicChannelOption.SO_RCVBUF,1024*1024)+" putt");
            System.out.println("SO_RCVBUF "+config.getOptions().get(QuicChannelOption.SO_RCVBUF));
            System.out.println("SO_SNDBUF "+config.getOptions().get(QuicChannelOption.SO_SNDBUF));

            System.out.println("setRecvByteBufAllocator "+config.getOptions().get(QuicChannelOption.RCVBUF_ALLOCATOR));

            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
                logger.info("Server socket closed. " + (future.isSuccess() ? "" : "Cause: " + future.cause()));
            });
            logger.info("LISTENING ON {}:{} FOR INCOMING CONNECTIONS",self.getHostName(),self.getPort());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}