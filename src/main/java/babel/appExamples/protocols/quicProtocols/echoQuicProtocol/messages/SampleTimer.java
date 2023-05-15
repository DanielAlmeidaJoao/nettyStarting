package babel.appExamples.protocols.quicProtocols.echoQuicProtocol.messages;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class SampleTimer extends ProtoTimer {
    public static final short TIMER_ID = 101;

    public SampleTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return null;
    }
}
