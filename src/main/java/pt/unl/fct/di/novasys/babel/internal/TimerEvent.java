package pt.unl.fct.di.novasys.babel.internal;

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

import java.util.Comparator;

public class TimerEvent extends InternalEvent implements Comparable<TimerEvent>, Comparator<TimerEvent> {

    private final long uuid;
    private final ProtoTimer timer;

    private final GenericProtocol consumer;
    private final boolean periodic;
    private final long period;

    private short protoTimerId;
    private boolean isCancelled;

    public TimerEvent(short protoTimerId,ProtoTimer timer, long uuid, GenericProtocol consumer, boolean periodic,
                      long period) {
        super(EventType.TIMER_EVENT);
        this.timer = timer;
        this.uuid = uuid;
        this.consumer = consumer;
        this.period = period;
        this.periodic = periodic;
        this.protoTimerId = protoTimerId;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public short getProtoTimerId(){
        return protoTimerId;
    }
    public ProtoTimer getTimer() {
        return timer;
    }

    @Override
    public String toString() {
        return "TimerEvent{" +
                "uuid=" + uuid +
                ", timer=" + timer +
                ", consumer=" + consumer +
                ", periodic=" + periodic +
                ", period=" + period +
                '}';
    }

    public long getUuid() {
        return uuid;
    }

    public long getPeriod() {
        return period;
    }

    public boolean isPeriodic() {
        return periodic;
    }

    public GenericProtocol getConsumer() {
        return consumer;
    }

    @Override
    public int compareTo(TimerEvent o) {
        return Long.compare(this.uuid, o.uuid);
    }

    @Override
    public int compare(TimerEvent o1, TimerEvent o2) {
        return Long.compare(o1.uuid, o2.uuid);
    }

}
