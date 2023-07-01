package tcpSupport.tcpStreamingAPI.utils;

import io.netty.channel.EventLoop;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.channels.SendBytesInterface;
import quicSupport.utils.enums.TransmissionType;

import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SendStreamContinuoslyLogics {

    ConcurrentLinkedQueue<Pair<InputStream,String>> linkedQueue;
    private ScheduledFuture scheduledFuture=null;
    private final SendBytesInterface sendBytesInterface;

    public SendStreamContinuoslyLogics(SendBytesInterface send) {
        this.sendBytesInterface = send;
    }

    private void startIteratingStreams(){
        for (Pair<InputStream,String> streamConIdPair : linkedQueue) {
            try{
                int available = streamConIdPair.getKey().available();
                if(available>0){
                    System.out.println("AVAILABLE READ "+available);
                    byte data [] = new byte[available];
                    streamConIdPair.getLeft().read(data,0,available);
                    sendBytesInterface.send(streamConIdPair.getRight(),data,available, TransmissionType.UNSTRUCTURED_STREAM);
                }
            }catch (Exception e){
                linkedQueue.remove(streamConIdPair);
            }
        }
        synchronized (this){
            if(linkedQueue.isEmpty()&&scheduledFuture!=null){
                scheduledFuture.cancel(false);
                scheduledFuture = null;
            }
        }
    }
    public void addToStreams(InputStream inputStream, String conId, EventLoop loop){
        synchronized (this){
            if(linkedQueue ==null){
                linkedQueue = new ConcurrentLinkedQueue<>();
            }
            for (Pair<InputStream, String> stream : linkedQueue) {
                if(stream.getLeft()==inputStream){
                    System.out.println("STREAM ALREADY REGISTERED");
                    return;
                }
            }
            linkedQueue.add(Pair.of(inputStream,conId));
            if(scheduledFuture!=null) return;
            scheduledFuture = loop.scheduleAtFixedRate(() -> startIteratingStreams(),1L,1L, TimeUnit.SECONDS);
        }
    }
}
