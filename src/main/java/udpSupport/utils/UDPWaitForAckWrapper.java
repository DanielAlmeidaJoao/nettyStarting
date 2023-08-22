package udpSupport.utils;

import java.util.concurrent.ScheduledFuture;

public class UDPWaitForAckWrapper {

    public final long timeStart;
    public ScheduledFuture scheduledFuture;

    public UDPWaitForAckWrapper(ScheduledFuture scheduledFuture){
        timeStart = System.currentTimeMillis();
        this.scheduledFuture = scheduledFuture;
    }

}

