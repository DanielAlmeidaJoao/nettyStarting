package udpSupport.utils;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.modelmapper.ModelMapper;

public class UDPLogics {
    public static final Gson gson = new Gson();

    public static final ModelMapper modelMapper = new ModelMapper();

    /*** MESSAGE CODES ***/
    public final static byte APP_MESSAGE = 'M';
    public final static byte APP_ACK = 'A';

    /*** METRIC TYPES ***/

    public static DatagramPacket datagramPacket(MessageWrapper messageWrapper){
        ByteBuf buf = Unpooled.buffer(messageWrapper.getData().length+9);
        buf.writeByte(messageWrapper.getMsgCode());
        buf.writeLong(messageWrapper.getMsgId());
        buf.writeBytes(messageWrapper.getData());
        return new DatagramPacket(buf,messageWrapper.getDest());
    }

}
