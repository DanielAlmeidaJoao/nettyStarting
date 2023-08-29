package quicSupport.client_server;/*
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.handlers.pipeline.QuicClientChannelConHandler;
import quicSupport.handlers.pipeline.QuicStreamInboundHandler;
import quicSupport.utils.LoadCertificate;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;
import tcpSupport.tcpChannelAPI.connectionSetups.ClientInterface;
import tcpSupport.tcpChannelAPI.utils.TCPChannelUtils;

import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

public final class QUICClientEntity implements ClientInterface {
    //private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QUICServerEntity.class);
    private static final Logger logger = LogManager.getLogger(QUICClientEntity.class);
    private QuicChannel quicChannel;
    private final InetSocketAddress self;
    private final CustomQuicChannelConsumer consumer;
    private NioEventLoopGroup group;
    private QuicSslContext context;
    public final Properties properties;

    public QUICClientEntity(InetSocketAddress self, CustomQuicChannelConsumer consumer, NioEventLoopGroup g, Properties properties){
        this.self = self;
        this.consumer = consumer;
        //
        this.group = g;
        context = null;
        this.properties = properties;
    }

    private TrustManagerFactory serverTrustManager() throws Exception {
        String s_keystoreFilename = properties.getProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY);//"keystore.jks";
        String s_keystorePassword = properties.getProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY);//"simple";
        return QUICLogics.trustManagerFactory(s_keystoreFilename,s_keystorePassword);
    }

    private ChannelHandler getCodec()throws Exception{
        if(context==null){
            String keystoreFilename = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY); //"keystore2.jks";
            String keystorePassword = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY);//"simple";
            String alias = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_ALIAS_KEY);//"clientcert";
            Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);

            context = QuicSslContextBuilder.forClient().
                    keyManager(pair.getRight(),null, (X509Certificate) pair.getLeft())
                    //trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .trustManager(serverTrustManager())
                    .applicationProtocols("QUIC").earlyData(true)
                    .build();
        }

        QuicClientCodecBuilder clientCodecBuilder =  new QuicClientCodecBuilder()
                .sslContext(context);
        clientCodecBuilder = (QuicClientCodecBuilder) QUICLogics.addConfigs(clientCodecBuilder,properties);
        return clientCodecBuilder.build();
    }
    public void connect(InetSocketAddress remote, TransmissionType transmissionType, String id, short destProto) throws Exception{
        Bootstrap bs = new Bootstrap();

        Channel channel = bs.group(group)
                .channel(NioDatagramChannel.class)
                .option(QuicChannelOption.RCVBUF_ALLOCATOR,new FixedRecvByteBufAllocator(65*1024))
                .handler(getCodec())
                .bind(0).sync().channel();
        QuicChannelBootstrap b = QuicChannel.newBootstrap(channel)
                .handler(new QuicClientChannelConHandler(self,remote,consumer,transmissionType,destProto))
                .streamHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new QuicStreamInboundHandler(consumer,id, QUICLogics.OUTGOING_CONNECTION));
                    }
                })
                .remoteAddress(remote)
                .attr(AttributeKey.valueOf(TCPChannelUtils.CUSTOM_ID_KEY),id)
                .attr(AttributeKey.valueOf(TCPChannelUtils.DEST_STREAM_PROTO),destProto);

                //.earlyDataSendCallBack(new CustomEarlyDataSendCallback(self,remote,consumer,metrics))
                b.connect().addListener(future -> {
                    if(!future.isSuccess()){
                        consumer.handleOpenConnectionFailed(remote,future.cause(), transmissionType,id);
                    }
                });
        //return channel.id().asShortText();
    }

    public void shutDown(){
        group.shutdownGracefully();
    }
}