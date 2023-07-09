package tcpSupport.tcpStreamingAPI.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import quicSupport.channels.SendStreamInterface;
import quicSupport.utils.enums.TransmissionType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class BabelInputStream {
    public final String streamId;
    public final SendStreamInterface streamInterface;
    private boolean flush;
    BabelInputStream(String streamId, SendStreamInterface streamInterface){
        this.streamId = streamId;
        this.streamInterface = streamInterface;
        flush = false;
    }

    public void setFlushMode(boolean newFlushMode){
        flush = newFlushMode;
    }

    public void sendInt(int value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(Integer.BYTES).writeInt(value),flush);
    }
    public void sendShort(short value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(Short.BYTES).writeShort(value),flush);
    }
    public void sendByte(byte value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(Byte.BYTES).writeByte(value),flush);
    }
    public void sendBoolean(boolean value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(1).writeBoolean(value),flush);
    }
    public void sendLong(long value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(Long.BYTES).writeLong(value),flush);
    }
    public void sendFloat(float value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(Float.BYTES).writeFloat(value),flush);
    }
    public void sendDouble(int value){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(Double.BYTES).writeDouble(value),flush);
    }
    public void sendBytes(byte [] bytes,int srcIndex,int len){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(len).writeBytes(bytes,srcIndex,len),flush);
    }
    public boolean sendBabelOutputStream(BabelOutputStream babelOutputStream){
        if(babelOutputStream.readableBytes()>0){
            streamInterface.sendStream(streamId,babelOutputStream.getBuffer().retainedSlice(),flush);
            return true;
        }
        return false;
    }
    public void sendBytes(byte [] bytes){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(bytes.length).writeBytes(bytes),flush);
    }
    public void sendBytes(ByteBuf buf, int srcIndex, int len){
        streamInterface.sendStream(streamId, Unpooled.directBuffer(len).writeBytes(buf,srcIndex,len),flush);
    }
    public void sendFile(File file) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(file);
        sendInputStream(inputStream,file.length());

    }
    public void sendInputStream(InputStream inputStream, long len){
        streamInterface.sendInputStream(streamId,inputStream,len);
    }
    public void flushStream(){
        streamInterface.flushStream(streamId);
    }
    public boolean getFlushMode(){
        return flush;
    }

    public static BabelInputStream toBabelStream(String conId,@NonNull SendStreamInterface streamInterface, TransmissionType type){
        if(TransmissionType.UNSTRUCTURED_STREAM==type){
            return new BabelInputStream(conId,streamInterface);
        }
        return null;
    }
}
