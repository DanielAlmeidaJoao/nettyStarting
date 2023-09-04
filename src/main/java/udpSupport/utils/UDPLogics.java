package udpSupport.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;

public class UDPLogics {
    //public static final Gson gson = new Gson();
    public static final int MAX_UDP_PAYLOAD_SIZE = 60000;

    /*** MESSAGE CODES ***/
    //public final static byte APP_MESSAGE = 'M';
    public final static byte SINGLE_MESSAGE = 'S';
    public final static byte STREAM_MESSAGE = 'T';


    public final static byte APP_ACK = 'A';

    /*** METRIC TYPES ***/

    public static final String MESSAGE_STAT_KEY="MS";
    public static final String ACK_STAT_KEY="AS";
    public static final String DELIVERED_MESSAGE_STATS="EMS";

    public static DatagramPacket datagramPacket(MessageWrapper messageWrapper){
        ByteBuf buf = Unpooled.buffer(messageWrapper.getData().length+9);
        buf.writeByte(messageWrapper.getMsgCode());
        buf.writeLong(messageWrapper.getMsgId());
        buf.writeBytes(messageWrapper.getData());
        return new DatagramPacket(buf,messageWrapper.getDest());
    }

}
