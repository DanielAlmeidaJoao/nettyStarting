package udpSupport.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.testing.TestQuicChannel;
import udpSupport.channels.SingleThreadedUDPChannel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class TestUDPChannel extends SingleThreadedUDPChannel {
    private static final Logger logger = LogManager.getLogger(TestUDPChannel.class);

    private FileOutputStream fos;
    public TestUDPChannel(Properties properties) throws Exception {
        super(properties);
        fos = new FileOutputStream("UDP_MOVIE_FILE.MP4");

    }

    int total = 0;
    @Override
    public void onDeliverMessage(byte[] message, InetSocketAddress from) {
        total += message.length;
        try{
            fos.write(message, 0, message.length);
            fos.flush();
            if(total>=1035368729){
                fos.close();
                System.out.println("FILE CLOSEDDDDDDDDDDDD "+total);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest) {
        if(!success){
            error.printStackTrace();
        }
    }

    public void startStreaming(InetSocketAddress peer){
        System.out.println("STREAMING STARTED!!!");
        try{
            //String p = "/home/tsunami/Downloads/Avatar The Way Of Water (2022) [1080p] [WEBRip] [5.1] [YTS.MX]/Avatar.The.Way.Of.Water.2022.1080p.WEBRip.x264.AAC5.1-[YTS.MX].mp4";
            Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            //Path filePath = Paths.get(p);
            //
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 32*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read, totalSent = 0;
            int cc = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                sendMessage(bytes,peer);
                cc++;
                if(cc>100){
                    cc=0;
                    //Thread.sleep(1000);
                    System.out.println("UP");
                }
                bytes = new byte[bufferSize];
            }
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
