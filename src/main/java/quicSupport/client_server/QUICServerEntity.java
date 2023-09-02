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

import appExamples2.appExamples.channels.FactoryMethods;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.incubator.codec.quic.*;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.handlers.pipeline.QuicServerChannelConHandler;
import quicSupport.handlers.pipeline.QuicStreamInboundHandler;
import quicSupport.utils.LoadCertificate;
import quicSupport.utils.QUICLogics;
import tcpSupport.tcpChannelAPI.connectionSetups.ServerInterface;
import tcpSupport.tcpChannelAPI.connectionSetups.TCPServerEntity;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;
import udpSupport.client_server.NettyUDPServer;

import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

public final class QUICServerEntity implements ServerInterface {
    private final CustomQuicChannelConsumer consumer;
    private final InetSocketAddress self;
    private final Properties properties;

    private Channel quicChannel;
    private EventLoopGroup group;
    //private static final Logger logger = LogManager.getLogger(QUICServerEntity.class);

    public QUICServerEntity(String host, int port, CustomQuicChannelConsumer consumer, Properties properties) {
        this.consumer = consumer;
        self = new InetSocketAddress(host,port);
        this.properties=properties;
        quicChannel =null;
    }
    private TrustManagerFactory clientTrustManager() throws Exception {
        String keystoreFilename = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY);
        String keystorePassword = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY);
        return QUICLogics.trustManagerFactory(keystoreFilename,keystorePassword);
    }
    private QuicSslContext getSignedSslContext() throws Exception {
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

    public ChannelHandler getChannelHandler(QuicSslContext context, int bufferSize) {
        QuicServerCodecBuilder serverCodecBuilder =  new QuicServerCodecBuilder()
                .sslContext(context);
        serverCodecBuilder = (QuicServerCodecBuilder) QUICLogics.addConfigs(serverCodecBuilder,properties);
        ChannelHandler codec = serverCodecBuilder
                // Setup a token handler. In a production system you would want to implement and provide your custom
                // one.
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                // ChannelHandler that is added into QuicChannel pipeline.
                .handler(new QuicServerChannelConHandler(consumer))
                .option(QuicChannelOption.RCVBUF_ALLOCATOR,new FixedRecvByteBufAllocator(bufferSize))
                .streamHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new QuicStreamInboundHandler(consumer,consumer.nextId(), QUICLogics.INCOMING_CONNECTION));
                    }
                })
                .build();
        return codec;
    }

    public void startServer() throws Exception {
        QuicSslContext context = getSignedSslContext();
        int serverThreads = FactoryMethods.serverThreads(properties);
        group = TCPServerEntity.createNewWorkerGroup(serverThreads);
        final int bufferSize = Integer.parseInt((String) properties.getOrDefault(TCPChannelUtils.BUFF_ALOC_SIZE,"16384"));
        ChannelHandler codec = getChannelHandler(context,bufferSize);
        Bootstrap bs = new Bootstrap();

        quicChannel = bs.group(group)
                .channel(NettyUDPServer.socketChannel())
                /*
                Allocates a new receive buffer whose capacity is probably large enough to read all inbound data
                and small enough not to waste its space.
                */
                //.option(EpollChannelOption.MAX_DATAGRAM_PAYLOAD_SIZE,2048)
                .option(QuicChannelOption.RCVBUF_ALLOCATOR,new FixedRecvByteBufAllocator(bufferSize))
                .handler(codec)
                .bind(self).sync()
                .addListener(future -> {
                    consumer.onServerSocketBind(future.isSuccess(),future.cause());
                })
                .channel();

        quicChannel.closeFuture().addListener(future -> {
            group.shutdownGracefully().getNow();
        });
    }
    public void shutDown(){
        if(quicChannel !=null){
            quicChannel.close();
            quicChannel.disconnect();
        }
    }
}