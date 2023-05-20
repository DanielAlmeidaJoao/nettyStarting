package appExamples2.appExamples.protocols.quicProtocols.echoQuicProtocol.messages;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class SampleTimer2 extends ProtoTimer {
    public static final short TIMER_ID = 104;

    public SampleTimer2() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return null;
    }
}
