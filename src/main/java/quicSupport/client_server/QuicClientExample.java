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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.handlers.QuicDelimitedMessageDecoder;
import quicSupport.handlers.funcHandlers.QuicListenerExecutor;
import quicSupport.handlers.server.QuicStreamReadHandler;
import quicSupport.handlers.server.ServerChannelInitializer;
import quicSupport.utils.LoadCertificate;
import quicSupport.handlers.client.QuicChannelConHandler;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public final class QuicClientExample {
    //private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(QuicServerExample.class);
    private static final Logger logger = LogManager.getLogger(QuicClientExample.class);
    public static final int DEFAULT_IDLE_TIMEOUT = 60*30;
    private QuicChannel quicChannel;
    private final InetSocketAddress self;
    private NioEventLoopGroup group;
    private Map<Long,QuicStreamChannel> streams;
    private QuicListenerExecutor streamListenerExecutor;

    public QuicClientExample(InetSocketAddress self, QuicListenerExecutor streamListenerExecutor, NioEventLoopGroup g){
        this.self = self;
        this.streamListenerExecutor = streamListenerExecutor;
        //
        this.group = g;
        streams = new HashMap<>();
    }
    public ChannelHandler getCodec()throws Exception{
        String keystoreFilename = "keystore2.jks";
        String keystorePassword = "simple";
        String alias = "clientcert";
        Pair<Certificate, PrivateKey> pair = LoadCertificate.getCertificate(keystoreFilename,keystorePassword,alias);

        QuicSslContext context = QuicSslContextBuilder.forClient().
                //keyManager(pair.getRight(),null, (X509Certificate) pair.getLeft())
                trustManager(InsecureTrustManagerFactory.INSTANCE)
                //trustManager((X509Certificate) pair.getLeft())
                .applicationProtocols("QUIC")
                .build();
        ChannelHandler codec = new QuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(DEFAULT_IDLE_TIMEOUT, TimeUnit.SECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .initialMaxStreamsUnidirectional(100)
                .build();
        return codec;
    }
    private void closeConnection(){
        quicChannel.close();
    }
    public void connect(InetSocketAddress remote) throws Exception{
        Bootstrap bs = new Bootstrap();
        Channel channel = bs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(getCodec())
                .bind(0).sync().channel();
        quicChannel = QuicChannel.newBootstrap(channel)
                .handler(new QuicChannelConHandler(self,remote,streamListenerExecutor))
                .streamHandler(new ServerChannelInitializer(streamListenerExecutor))
                .remoteAddress(remote)
                .connect().addListener(future -> {
                    if(future.isSuccess()){
                        logger.info("CLIENT CONNECTED TO {}",remote);
                    }else{
                        logger.info("CLIENT NOT CONNECTED TO {}",remote);
                        streamListenerExecutor.onConnectionError(remote,future.cause());
                    }
                })
                .get();
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