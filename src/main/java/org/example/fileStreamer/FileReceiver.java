package org.example.fileStreamer;

import org.example.server.StreamReceiver;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class FileReceiver {

    private Map<String,FileOutputStream> files;
    private StreamReceiver streamReceiver;
    private int port;

    public FileReceiver(int port){
        this.port = port;
        try {
            //String inputFileName = "/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4";
            files = new HashMap<>();
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }
    private void initChannel(String channelId){
        //
        System.out.println("CHANNEL ACTIVE!!!");
        try {
            files.put(channelId,new FileOutputStream(channelId+".mp4"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void writeToFile(String id, byte [] data){
        try{
            FileOutputStream fos = files.get(id);
            fos.write(data, 0, data.length);
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void closeFile(String id){
        System.out.println("CONNECTION CLOSED: "+id);
        try{
            files.get(id).close();
            //streamReceiver.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            streamReceiver = new StreamReceiver("localhost",port,this::initChannel,this::writeToFile,this::closeFile);
            streamReceiver.startListening();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + StreamReceiver.class.getSimpleName() +
                            " <port>");
        }
        int port = Integer.parseInt(args[0]);
        new FileReceiver(port).start();
        System.out.println("MAIN THREAAD GONE!");
    }
}
