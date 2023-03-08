package babel.appExamples.channels.messages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PrependHeaders {

    private ByteBuf tmp;
    private int currentLength;

    public PrependHeaders(){
        tmp = Unpooled.buffer();
        resetCurrentLength();

    }
    public void resetCurrentLength(){
        currentLength = -1;
    }


}
