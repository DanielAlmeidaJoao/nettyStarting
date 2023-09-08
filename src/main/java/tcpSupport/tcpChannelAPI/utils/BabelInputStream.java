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

    public void writeInt(int value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Integer.BYTES).writeInt(value),flush);
    }
    public void writeShort(short value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Short.BYTES).writeShort(value),flush);
    }
    public void writeByte(byte value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Byte.BYTES).writeByte(value),flush);
    }
    public void writeBoolean(boolean value){
        streamInterface.sendStream(streamId, alloc.directBuffer(1).writeBoolean(value),flush);
    }
    public void writeLong(long value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Long.BYTES).writeLong(value),flush);
    }
    public void writeFloat(float value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Float.BYTES).writeFloat(value),flush);
    }
    public void writeDouble(int value){
        streamInterface.sendStream(streamId, alloc.directBuffer(Double.BYTES).writeDouble(value),flush);
    }
    public void writeBytes(byte [] bytes, int srcIndex, int len){
        streamInterface.sendStream(streamId, alloc.directBuffer(len).writeBytes(bytes,srcIndex,len),flush);
    }
    public boolean writeBabelOutputStream(BabelOutputStream babelOutputStream){
        if(babelOutputStream.readableBytes()>0){
            streamInterface.sendStream(streamId,babelOutputStream.getBuffer().retainedDuplicate(),flush);
            return true;
        }
        return false;
    }
    public void writeBytes(byte [] bytes){
        streamInterface.sendStream(streamId, alloc.directBuffer(bytes.length).writeBytes(bytes),flush);
    }
    public void writeBytes(ByteBuf buf){
        streamInterface.sendStream(streamId,buf,flush);
    }
    public ByteBuf allocate(int size){
        return alloc.directBuffer(size);
    }
    public ByteBuf allocate(){
        return alloc.directBuffer();
    }
    public void writeFile(File file) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(file);
        writeInputStream(inputStream,file.length());

    }
    public void writeInputStream(InputStream inputStream, long len){
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
