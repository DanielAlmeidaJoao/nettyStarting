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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.handlers.channelFuncHandlers.SocketBindHandler;
import quicSupport.handlers.pipeline.QuicServerChannelConHandler;
import quicSupport.handlers.pipeline.QuicStreamInboundHandler;
import quicSupport.utils.LoadCertificate;
import quicSupport.utils.QUICLogics;

import javax.net.ssl.TrustManagerFactory;
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

    private Channel quicChannel;
    private static final Logger logger = LogManager.getLogger(QuicServerExample.class);


    public QuicServerExample(String host, int port, CustomQuicChannelConsumer consumer, Properties properties) {
        this.consumer = consumer;
        self = new InetSocketAddress(host,port);
        started = false;
        this.properties=properties;
        quicChannel =null;
    }
    private TrustManagerFactory clientTrustManager() throws Exception {
        String keystoreFilename = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY);
        String keystorePassword = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY);
        return QUICLogics.trustManagerFactory(keystoreFilename,keystorePassword);
    }
    public QuicSslContext getSignedSslContext() throws Exception {
        String keystoreFilename = properties.getProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY);//"keystore.jks";
        String keystorePassword = properties.getProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY);//"simple";
        String alias = properties.getProperty(QUICLogics.SERVER_KEYSTORE_ALIAS_KEY);//"quicTestCert";
        //String alias = "wservercert";
        Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);
        return QuicSslContextBuilder
                .forServer(pair.getRight(), null, (X509Certificate) pair.getLeft())
                .trustManager(clientTrustManager())
                .applicationProtocols("QUIC").earlyData(true)
                .build();
    }

    public ChannelHandler getChannelHandler(QuicSslContext context) {
        QuicServerCodecBuilder serverCodecBuilder =  new QuicServerCodecBuilder()
                .sslContext(context);
        serverCodecBuilder = (QuicServerCodecBuilder) QUICLogics.addConfigs(serverCodecBuilder,properties);
        ChannelHandler codec = serverCodecBuilder
                // Setup a token handler. In a production system you would want to implement and provide your custom
                // one.
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                // ChannelHandler that is added into QuicChannel pipeline.
                .handler(new QuicServerChannelConHandler(consumer))
                .streamHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new QuicStreamInboundHandler(consumer,consumer.nextId(), QUICLogics.INCOMING_CONNECTION));
                    }
                })
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
        NioEventLoopGroup group = new NioEventLoopGroup(4);
        ChannelHandler codec = getChannelHandler(context);
        try {
            Bootstrap bs = new Bootstrap();

            quicChannel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    /*
                    Allocates a new receive buffer whose capacity is probably large enough to read all inbound data
                    and small enough not to waste its space.
                    */
                    .option(QuicChannelOption.RCVBUF_ALLOCATOR,new FixedRecvByteBufAllocator(1024*65))
                    .handler(codec)
                    .bind(self).sync()
                    .addListener(future -> {
                        handler.execute(future.isSuccess(),future.cause());
                    })
                    .channel();
            started=true;

            quicChannel.closeFuture().addListener(future -> {
                System.out.println("QUIC SERVER DOWN");
                group.shutdownGracefully().getNow();
                logger.info("Server socket closed. " + (future.isSuccess() ? "" : "Cause: " + future.cause()));
            });
            logger.info("LISTENING ON {}:{} FOR INCOMING CONNECTIONS",self.getHostName(),self.getPort());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void closeServer(){
        if(quicChannel !=null){
            quicChannel.close();
            quicChannel.disconnect();
        }
    }
}