package pt.unl.fct.di.novasys.babel.core;

import pt.unl.fct.di.novasys.babel.internal.TimerEvent;

import java.util.concurrent.Callable;

public class TimerCallable<ProtoTimer> implements Callable<ProtoTimer> {

    private ProtoTimer protoTimer;
    private TimerEvent timerEvent;
    private GenericProtocol genericProtocol;
    public TimerCallable(ProtoTimer protoTimer, TimerEvent timerEvent, GenericProtocol consumer){
        this.protoTimer = protoTimer;
        this.timerEvent = timerEvent;
        this.genericProtocol = consumer;
    }
    @Override
    public ProtoTimer call() throws Exception {
        genericProtocol.deliverTimer(timerEvent);
        return protoTimer;
    }
}
