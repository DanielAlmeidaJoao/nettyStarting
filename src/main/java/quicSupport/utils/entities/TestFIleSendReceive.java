package quicSupport.utils.entities;

import io.netty.incubator.codec.quic.QuicStreamChannel;
import quicSupport.utils.Logics;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestFIleSendReceive {

    public static void startStreaming(QuicStreamChannel channel){
        try{
            // TODO: UNCOMMENTstreamSender.connect(host,port);
            Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Downloads/dieHart/Die.Hart.The.Movie.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 2*128*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read=0, totalSent = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                channel.writeAndFlush(Logics.writeBytes(read,bytes,Logics.APP_DATA));
            }
            Thread.sleep(2*1000);
            channel.close();
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
