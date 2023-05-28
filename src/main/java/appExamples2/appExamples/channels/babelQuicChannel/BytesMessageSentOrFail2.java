package appExamples2.appExamples.channels.babelQuicChannel;

import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class BytesMessageSentOrFail2 extends ProtoMessage {
    public static final short ID=13;
    public final byte [] data;
    public final int channelId;
    public BytesMessageSentOrFail2(byte [] data, int channelId) {
        super(ID);
        this.data=data;
        this.channelId=channelId;
    }
}
