package tcpSupport.tcpStreamingAPI.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import org.apache.commons.lang3.tuple.Pair;
import quicSupport.channels.SendStreamInterface;

import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SendStreamContinuoslyLogics {

    ConcurrentLinkedQueue<Pair<InputStream,String>> linkedQueue;
    private ScheduledFuture scheduledFuture=null;
    private final SendStreamInterface sendBytesInterface;
    private final long readPeriod;

    public SendStreamContinuoslyLogics(SendStreamInterface send, String readPeriodStr) {
        this.sendBytesInterface = send;
        if(readPeriodStr==null){
            this.readPeriod = 1000;
        }else{
            this.readPeriod = Long.parseLong(readPeriodStr);
        }

    }

    private void startIteratingStreams(){
        for (Pair<InputStream,String> streamConIdPair : linkedQueue) {
            try{
                int available = streamConIdPair.getKey().available();
                if(available>0){
                    byte data [] = new byte[available];
                    streamConIdPair.getLeft().read(data,0,available);
                    ByteBuf buf = Unpooled.directBuffer(data.length).writeBytes(data);
                    sendBytesInterface.sendStream(streamConIdPair.getRight(),buf,true);
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
                    return;
                }
            }
            linkedQueue.add(Pair.of(inputStream,conId));
            if(scheduledFuture!=null) return;
            scheduledFuture = loop.scheduleAtFixedRate(() -> startIteratingStreams(),0,readPeriod, TimeUnit.MILLISECONDS);
        }
    }
}
