package org.streamingAPI.fileStreamingApp;

import org.streamingAPI.server.StreamReceiver;
import org.streamingAPI.server.StreamReceiverImplementation;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class FileReceiver {

    private Map<String,FileOutputStream> files;
    private StreamReceiver streamReceiverLogic;
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

    private void firstBytesHandler(byte [] data){
        System.out.println("GOING TO PRINT CONTROL DATA!");
        if(data.length == 0){
            return;
        }
        String ola = new String(data);
        System.out.println(ola);
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
            streamReceiverLogic = new StreamReceiverImplementation("localhost",port,
                    this::initChannel,this::writeToFile,
                    this::closeFile,this::firstBytesHandler);
            streamReceiverLogic.startListening();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println(
                    "Usage: " + StreamReceiverImplementation.class.getSimpleName() +
                            " <port>");
        }
        int port = Integer.parseInt(args[0]);
        new FileReceiver(port).start();
        System.out.println("MAIN THREAAD GONE!");
    }
}