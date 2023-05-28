package appExamples2.appExamples.channels.babelQuicChannel;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class UnstructuredSreamSent extends ProtoMessage {
    public static final short ID=13;
    public final byte [] data;
    public UnstructuredSreamSent(byte [] msg) {
        super(ID);
        this.data=msg;
    }
}
