package tcpSupport.tcpChannelAPI.utils;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
    public byte [] readBytes(){
        byte [] b = new byte[buf.readableBytes()];
        buf.readBytes(b);
        buf.discardReadBytes();
        release();
        return b;
    }
    public int readBytes(byte[] dst, int dstIndex, int length){
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

    public void readBytes(OutputStream outputStream, int available) throws IOException {
        buf.readBytes(outputStream, available);
    }
    public void readBytes(OutputStream outputStream) throws IOException {
        buf.readBytes(outputStream,buf.readableBytes());
    }
    public void readBytes(ByteBuffer outputStream) throws IOException {
        buf.readBytes(outputStream);
    }
    public void readBytes(FileChannel out, long position, int length) throws IOException {
        buf.readBytes(out,position,length);
    }
    public void release(){
        buf.release();
    }
    public int readableBytes(){
        return buf.readableBytes();
    }
}
