package org.example.fileStreamer;

import org.example.server.StreamReceiver;

import java.io.FileOutputStream;

public class FileReceiver {

    private FileOutputStream fos;
    private int port;

    public FileReceiver(int port){
        this.port = port;
        try {
            //String inputFileName = "/home/tsunami/Downloads/Plane (2023) [720p] [WEBRip] [YTS.MX]/Plane.2023.720p.WEBRip.x264.AAC-[YTS.MX].mp4";
            fos = new FileOutputStream("ola2_movie.mp4");
        }catch (Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }
    private void writeToFile(String id, byte [] data){
        try{
            fos.write(data, 0, data.length);
            fos.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void closeFile(String id){
        System.out.println("CONNECTION CLOSED: "+id);
        try{
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            new StreamReceiver(port,this::writeToFile,this::closeFile).startListening();
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
