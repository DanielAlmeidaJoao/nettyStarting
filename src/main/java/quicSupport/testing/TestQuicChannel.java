package quicSupport.testing;

import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.channels.SingleThreadedQuicChannel;
import quicSupport.utils.metrics.QuicConnectionMetrics;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class TestQuicChannel extends SingleThreadedQuicChannel {

    private static final Logger logger = LogManager.getLogger(TestQuicChannel.class);
    //private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(TestQuicChannel.class);


    public TestQuicChannel(Properties properties) throws IOException {
        super(properties);
    }

    @Override
    public void onStreamErrorHandler(InetSocketAddress peer, Throwable error, String streamId) {

    }

    @Override
    public void onStreamClosedHandler(InetSocketAddress peer, String streamId) {

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

    @Override
    public void onStreamCreatedHandler(InetSocketAddress peer, String streamId) {
    }

    @Override
    public void onConnectionDown(InetSocketAddress peer, boolean incoming) {
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



    FileOutputStream fos = new FileOutputStream("TESTQUIC33.MP4");
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
    public void onConnectionUp(boolean incoming, InetSocketAddress peer) {

    }

    @Override
    public void onOpenConnectionFailed(InetSocketAddress peer, Throwable cause) {

    }
    @Override
    public void onMessageSent(byte[] message, int len, Throwable error,InetSocketAddress peer) {
        logger.info("FAILED TO SEND. REASON: {}",error.getLocalizedMessage());
    }
    public void startStreaming(InetSocketAddress peer){
        System.out.println("STREAMING STARTED!!!");
        try{
            String p = "/home/tsunami/Downloads/Avatar The Way Of Water (2022) [1080p] [WEBRip] [5.1] [YTS.MX]/Avatar.The.Way.Of.Water.2022.1080p.WEBRip.x264.AAC5.1-[YTS.MX].mp4";
            //Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            Path filePath = Paths.get(p);
            //
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 2*1024*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read, totalSent = 0;
            int cc = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                send(peer,bytes,read);
                cc++;
                if(cc>100){
                    cc=0;
                    Thread.sleep(1000);
                    System.out.println("UP");
                }
                bytes = new byte[bufferSize];
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
