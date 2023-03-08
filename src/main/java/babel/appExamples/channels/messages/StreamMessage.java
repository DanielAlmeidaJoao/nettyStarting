package babel.appExamples.channels.messages;

import lombok.Getter;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.babel.internal.BabelMessage;

@Getter
public class StreamMessage extends ProtoMessage {
    public static final short ID = 102;
    private byte data [];
    private int dataLength;
    private String streamId;
    public StreamMessage(byte [] data,int dataLength,String streamId) {
        super(ID);
        this.data=data;
        this.dataLength = dataLength;
        this.streamId=streamId;
    }
}
