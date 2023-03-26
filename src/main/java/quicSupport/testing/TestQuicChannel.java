package quicSupport.testing;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.streamingAPI.server.channelHandlers.messages.HandShakeMessage;
import pt.unl.fct.di.novasys.channel.tcp.events.ChannelMetrics;
import quicSupport.CustomQuicChannel;
import quicSupport.client_server.QuicServerExample;
import quicSupport.handlers.channelFuncHandlers.OldMetricsHandler;
import quicSupport.handlers.channelFuncHandlers.QuicConnectionMetricsHandler;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

public class TestQuicChannel extends CustomQuicChannel {

    private static final Logger logger = LogManager.getLogger(TestQuicChannel.class);
    //private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(TestQuicChannel.class);


    public TestQuicChannel(Properties properties) throws IOException {
        super(properties);
    }

    @Override
    public void onStreamErrorHandler(InetSocketAddress peer, QuicStreamChannel channel) {

    }

    @Override
    public void onStreamClosedHandler(InetSocketAddress peer, QuicStreamChannel channel) {

    }

    public void readStats(InetSocketAddress peer, QuicConnectionMetrics metrics){

    }
    public void getStats(InetSocketAddress peer){
        super.getStats(peer,this::readStats);
    }
    public void readOldMetrics(List<QuicConnectionMetrics> old){

    }
    public void oldMetrics(){
        super.oldMetrics(this::readOldMetrics);
    }

    QuicStreamChannel bb=null;
    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, QuicStreamChannel channel) {
        bb=channel;
    }

    public void setBb(){
        ((Runnable) () -> {
            while (bb.isActive()) {
                System.out.println("SENTD");
                bb.writeAndFlush(Unpooled.copiedBuffer("o".getBytes()));
            }
        }).run();
    }
    @Override
    public void onChannelClosed(InetSocketAddress peer) {
        try{
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void failedToCreateStream(InetSocketAddress peer, Throwable error) {

    }

    @Override
    public void failedToGetMetrics(Throwable cause) {

    }

    @Override
    public void failedToCloseStream(String streamId, Throwable reason) {

    }

    @Override
    public void failedToSend(InetSocketAddress host, byte[] message, int len, Throwable error) {

    }

    FileOutputStream fos = new FileOutputStream("TESTQUIC.MP4");
    @Override
    public void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from) {
        System.out.println("RECEIVED: "+bytes.length);
        try{
            fos.write(bytes, 0, bytes.length);
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        //System.out.println(new String(bytes));
    }

    @Override
    public void onChannelActive(Channel channel, HandShakeMessage handShakeMessage, InetSocketAddress peer) {

    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {

    }

    @Override
    public void failedToSend(String streamId, byte[] message, int len, Throwable error) {

    }

    public static void main(String args []) throws IOException {
        logger.info("STARREDD");
        Properties properties = new Properties();
        properties.setProperty("address","localhost");
        String port = args[0];
        properties.setProperty("port",port);
        TestQuicChannel testQuicChannel = new TestQuicChannel(properties);
        if("8081".equals(port)){
            InetSocketAddress remote = new InetSocketAddress("localhost",8082);
            testQuicChannel.openConnection(remote);
        }
    }

}
