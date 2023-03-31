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
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.CustomQuicChannelConsumer;
import quicSupport.handlers.pipeline.ServerChannelInitializer;
import quicSupport.handlers.pipeline.QuicClientChannelConHandler;
import quicSupport.utils.Logics;
import quicSupport.utils.metrics.QuicChannelMetrics;

import java.net.InetSocketAddress;
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

    public QuicClientExample(InetSocketAddress self, CustomQuicChannelConsumer consumer, NioEventLoopGroup g, QuicChannelMetrics metrics){
        this.self = self;
        this.consumer = consumer;
        //
        this.group = g;
        this.metrics = metrics;
        streams = new HashMap<>();
    }
    public ChannelHandler getCodec(Properties properties)throws Exception{
        String keystoreFilename = "keystore2.jks";
        String keystorePassword = "simple";
        String alias = "clientcert";
        //Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);
        QuicSslContext context = QuicSslContextBuilder.forClient().
                //keyManager(pair.getRight(),null, (X509Certificate) pair.getLeft())
                trustManager(InsecureTrustManagerFactory.INSTANCE)
                //trustManager((X509Certificate) pair.getLeft())
                .applicationProtocols("QUIC")
                .build();
        QuicClientCodecBuilder clientCodecBuilder =  new QuicClientCodecBuilder()
                .sslContext(context);
        clientCodecBuilder = (QuicClientCodecBuilder) Logics.addConfigs(clientCodecBuilder,properties);
        return clientCodecBuilder.build();
    }
    public void connect(InetSocketAddress remote, Properties properties) throws Exception{
        Bootstrap bs = new Bootstrap();
        Channel channel = bs.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK,new WriteBufferWaterMark(2*1024*1024,2*1024*1024*2))
                .option(ChannelOption.SO_RCVBUF,2*1024*1024)
                .option(ChannelOption.SO_SNDBUF,2*1024*1024)
                .handler(getCodec(properties))
                .bind(0).sync().channel();
        var chan = QuicChannel.newBootstrap(channel)
                //.option(ChannelOption.WRITE_BUFFER_WATER_MARK,new WriteBufferWaterMark(64*1024,128*1024))
                .handler(new QuicClientChannelConHandler(self,remote,consumer,metrics))
                .streamHandler(new ServerChannelInitializer(consumer,metrics,Logics.OUTGOING_CONNECTION))
                .remoteAddress(remote).connect().addListener(future -> {
            if(!future.isSuccess()){
                consumer.onOpenConnectionFailed(remote,future.cause());
            }
        }).get();
        var config = chan.config();
        System.out.println(config.getWriteBufferHighWaterMark());
        System.out.println(config.getWriteBufferLowWaterMark());

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
}