package udpSupport.test;

import io.netty.buffer.Unpooled;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import quicSupport.utils.QUICLogics;
import udpSupport.channels.SingleThreadedUDPChannel;
import udpSupport.channels.UDPChannel;
import udpSupport.channels.UDPChannelHandlerMethods;
import udpSupport.channels.UDPChannelInterface;
import udpSupport.metrics.ChannelStats;
import udpSupport.metrics.NetworkStats;
import udpSupport.metrics.NetworkStatsWrapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

public class TestUDPChannel<T> implements UDPChannelHandlerMethods<T> {
    private static final Logger logger = LogManager.getLogger(TestUDPChannel.class);

    private FileOutputStream fos;
    private UDPChannelInterface udpChannelInterface;
    public TestUDPChannel(Properties properties) throws Exception {
        if(properties.getProperty("SINLGE_TRHEADED")!=null){
            udpChannelInterface = new SingleThreadedUDPChannel(properties,this,null);
        }else {
            udpChannelInterface = new UDPChannel(properties,false,this,null);
        }
        fos = new FileOutputStream("UDP_MOVIE_FILE.MP4");
        System.out.println("SERVER STARTED ");

    }

    int total = 0;

    @Override
    public void onPeerDown(InetSocketAddress peer) {
        System.out.println("PEER DOWN "+peer);
    }

    @Override
    public void onDeliverMessage(T message, InetSocketAddress from) {
        /**
        total += message.readableBytes();
        System.out.println("RECEIVED "+total+" -- "+message.readableBytes());
        /* if(total>0){
            return;
        } */
        /**
        try{

            if(message.readableBytes()==bufferSize){
                receivedHashes.add(Hex.encodeHexString(QUICLogics.hash(message.array())));
            }else {
                System.out.println("ONLY ONCE "+message.readableBytes()+" "+total);
            }

            //fos.write(message, 0, message.length);
            //fos.flush();
            if(total>= 1035368729){
                //fos.close();
                System.out.println("FILE CLOSEDDDDDDDDDDDD "+total);
                sumHashes(receivedHashes);
                udpChannelInterface.readMetrics(this::onReadMetrics);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        **/
    }
    private void sumHashes(SortedSet<String> set){
        System.out.println("FILE CLOSEDDDDDDDDDDDD "+total);
        long sum = 0;
        for (String receivedHash : set) {
            sum += receivedHash.hashCode();
        }
        System.out.println(" SUMM "+sum);
    }
    @Override
    public void onMessageSentHandler(boolean success, Throwable error, byte[] message, InetSocketAddress dest) {
        if(!success){
            error.printStackTrace();
        }
    }
    SortedSet<String> sentHashes=new TreeSet<>();
    SortedSet<String> receivedHashes=new TreeSet<>();
    int bufferSize = 128*1024; // 8KB buffer size

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
            byte [] bytes = new byte[bufferSize];

            //ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read, totalSent = 0;
            int cc = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                if(read==bufferSize){
                    sentHashes.add(Hex.encodeHexString(QUICLogics.hash(bytes)));
                }else {
                    System.out.println("EXPECTED ONCE!!! "+read);
                }
                udpChannelInterface.sendMessage(Unpooled.copiedBuffer(bytes,0,read),peer);
                cc++;
                //Thread.sleep(1000);
                bytes = new byte[bufferSize];
            }
            System.out.println("TOTAL SENT "+totalSent+" COUNT -- "+cc);
            sumHashes(sentHashes);
            Thread.sleep(10000);
            //udpChannelInterface.readMetrics(stats -> onReadMetrics(stats));
            System.out.println("METRICS OUT ?");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void interact(){
        Scanner scanner = new Scanner(System.in);
        String input = "";
        while(!"quit".equalsIgnoreCase(input)){
            System.out.println("ENTER SOMETHING COMMAND:");
            input = scanner.nextLine();
            if("m".equalsIgnoreCase(input)){
                //udpChannelInterface.readMetrics(stats -> onReadMetrics(stats));
            } else if ("send".equalsIgnoreCase(input)) {
                System.out.println("Enter data:");
                input = scanner.nextLine();
                System.out.println("HOST NAME:");
                String host = scanner.nextLine();
                System.out.println("PORT");
                int p = scanner.nextInt();
                scanner.nextLine();
                InetSocketAddress address = new InetSocketAddress(host, p);
                udpChannelInterface.sendMessage(Unpooled.copiedBuffer(input.getBytes()),address);
                System.out.println("SENT "+input);
            }
        }
    }
    public void onReadMetrics(ChannelStats stats){
        for (NetworkStatsWrapper value : stats.getStatsMap().values()) {
            for (NetworkStats networkStats : value.statsCollection()) {
                //System.out.println(UDPLogics.gson.toJson(networkStats));
            }
        }
    }
}
