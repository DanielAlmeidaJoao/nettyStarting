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
import quicSupport.utils.Logics;
import quicSupport.utils.entities.QuicConnectionMetrics;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        System.out.println("GETTING THE STATS!!!");
        System.out.println(metrics);
    }
    public void getStats(InetSocketAddress peer){
        super.getStats(peer,this::readStats);
    }
    public void readOldMetrics(List<QuicConnectionMetrics> old){
        System.out.println(old);
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
            System.out.println("RECEIVED TOTAL "+total);
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
        System.out.println("FAILED TO GET THE STATS "+cause);
    }

    @Override
    public void failedToCloseStream(String streamId, Throwable reason) {

    }

    @Override
    public void failedToSend(InetSocketAddress host, byte[] message, int len, Throwable error) {

    }

    FileOutputStream fos = new FileOutputStream("TESTQUIC.MP4");
    int total = 0;
    @Override
    public void onChannelRead(String channelId, byte[] bytes, InetSocketAddress from) {
        total += bytes.length;
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
        System.out.println("FAILED TO SEND!!!! "+len);
    }
    public void startStreaming(InetSocketAddress peer){
        System.out.println("STREAMING STARTED!!!");
        try{
            Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 2*128*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read, totalSent = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                send(peer,bytes,read);
                //Thread.sleep(100);
            }
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
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
