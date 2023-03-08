package org.streamingAPI.fileStreamingApp;

import org.streamingAPI.client.StreamSender;
import org.streamingAPI.client.StreamSenderImplementation;
import org.streamingAPI.handlerFunctions.receiver.ChannelHandlers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileStreamer {
    private StreamSender streamSender;
    private FileOutputStream fileOutputStream;

    public FileStreamer(String host, int port){
        ChannelHandlers handlerFunctions = new ChannelHandlers(
                this::channelActive,
                this::channelActiveRead,
                this::channelRead,
                this::channelInactive
        );
        streamSender = new StreamSenderImplementation(host,port,handlerFunctions);
        try {
            fileOutputStream = new FileOutputStream("copyOFmine.mp4");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void startStreaming(){
        try{
            streamSender.connect();
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
                streamSender.sendBytes(bytes,read);
            }
            Thread.sleep(10*1000);
            streamSender.close();
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void initialData(byte [] data){

    }

    public static void main(String [] args ){
        if (args.length != 2) {
            System.err.println(
                    "Usage: " + StreamSenderImplementation.class.getSimpleName() +
                            " <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        new FileStreamer(host,port).startStreaming();
    }

    public void channelActive(String channelId){

    }
    public void channelActiveRead(String channelId,byte [] data){

    }
    public void channelRead(String channelId, byte [] data){
        try {
            fileOutputStream.write(data, 0, data.length);
            fileOutputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void channelInactive(String channelId){
        try {
            fileOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
