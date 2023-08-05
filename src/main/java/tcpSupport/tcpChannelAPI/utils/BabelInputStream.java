package tcpSupport.tcpChannelAPI.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
    private ByteBufAllocator alloc;
    private boolean flush;

    BabelInputStream(String streamId, SendStreamInterface streamInterface, ByteBufAllocator alloc){
        this.streamId = streamId;
        this.streamInterface = streamInterface;
        flush = false;
        this.alloc = alloc;
    }

    public void setFlushMode(boolean newFlushMode){
        flush = newFlushMode;
    }

    public void sendInt(int value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Integer.BYTES).writeInt(value),flush);
    }
    public void sendShort(short value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Short.BYTES).writeShort(value),flush);
    }
    public void sendByte(byte value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Byte.BYTES).writeByte(value),flush);
    }
    public void sendBoolean(boolean value){
        streamInterface.sendStream(streamId, alloc.directBuffer(1).writeBoolean(value),flush);
    }
    public void sendLong(long value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Long.BYTES).writeLong(value),flush);
    }
    public void sendFloat(float value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Float.BYTES).writeFloat(value),flush);
    }
    public void sendDouble(int value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Double.BYTES).writeDouble(value),flush);
    }
    public void sendBytes(byte [] bytes,int srcIndex,int len){
        streamInterface.sendStream(streamId, alloc.directBuffer(len).writeBytes(bytes,srcIndex,len),flush);
    }
    public boolean sendBabelOutputStream(BabelOutputStream babelOutputStream){
        if(babelOutputStream.readableBytes()>0){
            streamInterface.sendStream(streamId,babelOutputStream.getBuffer().retainedDuplicate(),flush);
            return true;
        }
        return false;
    }
    public void sendBytes(byte [] bytes){
        streamInterface.sendStream(streamId, alloc.directBuffer(bytes.length).writeBytes(bytes),flush);
    }
    public void sendBytes(ByteBuf buf, int srcIndex, int len){
        streamInterface.sendStream(streamId, alloc.directBuffer(len).writeBytes(buf,srcIndex,len),flush);
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

    public static BabelInputStream toBabelStream(String conId, @NonNull SendStreamInterface streamInterface, TransmissionType type, ByteBufAllocator alloc){
        if(TransmissionType.UNSTRUCTURED_STREAM==type){
            return new BabelInputStream(conId,streamInterface,alloc);
        }
        return null;
    }
}
