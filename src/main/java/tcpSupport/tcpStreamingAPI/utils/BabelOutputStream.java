package tcpSupport.tcpStreamingAPI.utils;

import io.netty.buffer.ByteBuf;

public class BabelOutputStream {

    private final ByteBuf buf;

    public BabelOutputStream(ByteBuf buf){
        this.buf = buf;
    }

    public int readInt(){
        return buf.readInt();
    }
    public boolean readBoolean(){
        return buf.readBoolean();
    }
    public double readDouble(){
        return buf.readDouble();
    }
    public float readFloat(){
        return buf.readFloat();
    }
    public byte readByte(){
        return buf.readByte();
    }
    public short readShort(){
        return buf.readShort();
    }
    public ByteBuf getBuffer(){
        return buf;
    }
    public byte [] readRemainingBytes(){
        byte [] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        release();
        return b;
    }
    public int readRemainingBytes(byte[] dst, int dstIndex, int length){
        int available = buf.readableBytes();
        if(length>available){
            length = available;
        }
        buf.readBytes(dst,dstIndex,length);
        if(buf.readableBytes()==0){
            release();
        }
        return length;
    }
    public void release(){
        buf.release();
    }
    public int readableBytes(){
        return buf.readableBytes();
    }
}
