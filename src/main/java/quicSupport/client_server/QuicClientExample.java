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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
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
import quicSupport.handlers.pipeline.ServerChannelInitializer;
import quicSupport.utils.LoadCertificate;
import quicSupport.utils.QUICLogics;
import quicSupport.utils.enums.TransmissionType;
import quicSupport.utils.metrics.QuicChannelMetrics;
import tcpSupport.tcpChannelAPI.utils.TCPStreamUtils;

import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

public final class QuicClientExample {
    //private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);
    private static final Logger logger = LogManager.getLogger(QuicClientExample.class);
    private QuicChannel quicChannel;
    private final InetSocketAddress self;
    private final CustomQuicChannelConsumer consumer;
    private NioEventLoopGroup group;
    private Map<Long,QuicStreamChannel> streams;
    private QuicChannelMetrics metrics;
    private QuicSslContext context;

    public QuicClientExample(InetSocketAddress self, CustomQuicChannelConsumer consumer, NioEventLoopGroup g){
        this.self = self;
        this.consumer = consumer;
        //
        this.group = g;
        this.metrics = metrics;
        streams = new HashMap<>();
        context = null;
    }

    private TrustManagerFactory serverTrustManager(Properties properties) throws Exception {
        String s_keystoreFilename = properties.getProperty(QUICLogics.SERVER_KEYSTORE_FILE_KEY);//"keystore.jks";
        String s_keystorePassword = properties.getProperty(QUICLogics.SERVER_KEYSTORE_PASSWORD_KEY);//"simple";
        return QUICLogics.trustManagerFactory(s_keystoreFilename,s_keystorePassword);
    }

    public ChannelHandler getCodec(Properties properties)throws Exception{
        String keystoreFilename = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_FILE_KEY); //"keystore2.jks";
        String keystorePassword = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_PASSWORD_KEY);//"simple";
        String alias = properties.getProperty(QUICLogics.CLIENT_KEYSTORE_ALIAS_KEY);//"clientcert";
        Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);
        if(context==null){
            context = QuicSslContextBuilder.forClient().
                    keyManager(pair.getRight(),null, (X509Certificate) pair.getLeft())
                    //trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .trustManager(serverTrustManager(properties))
                    .applicationProtocols("QUIC").earlyData(true)
                    .build();
        }

        QuicClientCodecBuilder clientCodecBuilder =  new QuicClientCodecBuilder()
                .sslContext(context);
        clientCodecBuilder = (QuicClientCodecBuilder) QUICLogics.addConfigs(clientCodecBuilder,properties);
        return clientCodecBuilder.build();
    }
    public String connect(InetSocketAddress remote, Properties properties, TransmissionType transmissionType, String id) throws Exception{
        Bootstrap bs = new Bootstrap();

        Channel channel = bs.group(group)
                .channel(NioDatagramChannel.class)
                .option(QuicChannelOption.RCVBUF_ALLOCATOR,new FixedRecvByteBufAllocator(65*1024))
                .handler(getCodec(properties))
                .bind(0).sync().channel();
        QuicChannel.newBootstrap(channel)
                .handler(new QuicClientChannelConHandler(self,remote,consumer, transmissionType))
                .streamHandler(new ServerChannelInitializer(consumer,QUICLogics.OUTGOING_CONNECTION))
                .remoteAddress(remote)
                .attr(AttributeKey.valueOf(TCPStreamUtils.CUSTOM_ID_KEY),id)
                //.earlyDataSendCallBack(new CustomEarlyDataSendCallback(self,remote,consumer,metrics))
                .connect().addListener(future -> {
            if(!future.isSuccess()){
                consumer.handleOpenConnectionFailed(remote,future.cause(), transmissionType,id);
            }
        });
        return channel.id().asShortText();
    }

    private QuicStreamChannel getOrThrow(long id){
        QuicStreamChannel stream = streams.get(id);
        if(stream==null){
            throw new NoSuchElementException(String.format("STREAM <%S> NOT FOUND!",id));
        }
        return stream;
    }
    public void send(long streamId, byte [] data){
        getOrThrow(streamId).writeAndFlush(Unpooled.copiedBuffer(data));
    }
    public void send(long streamId, ByteBuf buf){
        getOrThrow(streamId).writeAndFlush(buf);
    }

    public void closeClient(){
        group.shutdownGracefully();
    }
}