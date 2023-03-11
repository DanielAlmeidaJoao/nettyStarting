package babel.appExamples.channels.messages;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class EndOfStreaming extends ProtoMessage {
    public static final short ID = 103;
    public EndOfStreaming() {
        super(ID);
    }
}
