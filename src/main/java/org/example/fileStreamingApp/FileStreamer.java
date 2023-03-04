package org.example.fileStreamingApp;

import org.example.client.StreamSender;
import org.example.client.StreamSenderImplementation;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileStreamer {
    private StreamSender streamSender;

    public FileStreamer(String host, int port){
        streamSender = new StreamSenderImplementation(host,port);
    }
    public void startStreaming(){
        try{
            streamSender.connect();

            Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Desktop/danielJoao_CV (1).pdf");
            //Path filePath = Paths.get("C:\\Users\\Quim\\Documents\\danielJoao\\THESIS_PROJECT\\diehart.mp4");

            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            int bufferSize = 2*128*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read=0, totalSent = 0;
            while ( ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                totalSent += read;
                streamSender.sendBytes(bytes,read);
                bytes = new byte[bufferSize];
            }
            streamSender.printSomeConfigs();
            streamSender.close();
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
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
}
