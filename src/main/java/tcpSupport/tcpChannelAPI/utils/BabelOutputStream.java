package tcpSupport.tcpChannelAPI.utils;

import io.netty.buffer.ByteBuf;

public class BabelOutputStream {

    private final ByteBuf buf;
    private int available;

    public BabelOutputStream(ByteBuf buf, int readAble){
        this.buf = buf;
        this.available = readAble;
    }
    private void decAvailable(int bytes){
        available -= bytes;
    }
    public int readInt(){
        int res = buf.readInt();
        return res;
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
        buf.discardReadBytes();
        release();
        return b;
    }
    public int readRemainingBytes(byte[] dst, int dstIndex, int length){
        int available = buf.readableBytes();
        if(length>available){
            length = available;
        }
        buf.readBytes(dst,dstIndex,length);
        buf.discardReadBytes();
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
