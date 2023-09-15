package udpSupport.utils;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.ScheduledFuture;

public class UDPWaitForAckWrapper {

    public final long timeStart;
    public ByteBuf packet;

    public ByteBuf getPacket() {
        ByteBuf aux = packet;
        packet = null;
        return aux;
    }

    public ScheduledFuture scheduledFuture;

    public UDPWaitForAckWrapper(ScheduledFuture scheduledFuture, ByteBuf packet){
        this.packet = packet;
        timeStart = System.currentTimeMillis();
        this.scheduledFuture = scheduledFuture;
    }

}

