package org.example.fileStreamer;

import org.example.client.StreamSender;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileStreamer {

    private StreamSender streamSender;

    public FileStreamer(String host, int port){
        streamSender = new StreamSender(host,port);
    }
    public void startStreaming(){
        try{
            streamSender.connect();

            Path filePath = Paths.get("/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4");
            //Path filePath = Paths.get("/home/tsunami/Desktop/danielJoao_CV (1).pdf");
            FileInputStream fileInputStream = new FileInputStream(filePath.toFile());
            FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ);
            int bufferSize = 8*1024; // 8KB buffer size
            byte [] bytes = new byte[bufferSize];

            ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            int read=0, totalSent = 0;
            while(true){
                while ( streamSender.canSend() && ( ( read =  fileInputStream.read(bytes) ) != -1)) {
                    //buffer.flip();
                    // process buffer
                    totalSent += read;
                    //buffer.
                    if(read!=bytes.length){
                        System.out.println(read+ " "+bytes.length);
                        //System.exit(1);
                    }
                    streamSender.sendMessage(bytes,read);
                    bytes = new byte[bufferSize];
                    //buffer.clear();
                }
                if(read==-1){
                    break;
                }
            }

            streamSender.keepRunning();
            streamSender.printSomeConfigs();
            channel.close();
            System.out.println("TOTAL SENT "+totalSent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String [] args ){
        if (args.length != 2) {
            System.err.println(
                    "Usage: " + StreamSender.class.getSimpleName() +
                            " <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        new FileStreamer(host,port).startStreaming();
    }
}
