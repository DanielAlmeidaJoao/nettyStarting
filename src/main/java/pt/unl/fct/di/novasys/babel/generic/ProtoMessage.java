package pt.unl.fct.di.novasys.babel.generic;

import appExamples2.appExamples.channels.messages.BytesToBabelMessage;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Abstract Message class to be extended by protocol-specific messages.
 */
public abstract class ProtoMessage {

    private final short id;

    public ProtoMessage(short id){
        this.id = id;
    }

    public short getId() {
        return id;
    }

    private void setSerializer(ByteBuf out) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                switch (field.getType().getSimpleName()) {
                    case "byte":
                        out.writeByte(field.getByte(this));
                        break;
                    case "boolean":
                        out.writeBoolean(field.getBoolean(this));
                        break;
                    case "short":
                        out.writeShort(field.getByte(this));
                        break;
                    case "int":
                        out.writeInt(field.getInt(this));
                        break;
                    case "float":
                        out.writeFloat(field.getFloat(this));
                        break;
                    case "long":
                        out.writeLong(field.getLong(this));
                        break;
                    case "char":
                        out.writeChar(field.getChar(this));
                        break;
                    case "string":
                        out.writeCharSequence((String) field.get(this), Charset.defaultCharset());
                        break;
                    case "double":
                        out.writeDouble(field.getDouble(this));
                        break;
                    case "byte[]":
                        out.writeBytes((byte[]) field.get(this));
                        break;
                    default:
                        Object object = field.get(this);
                        if (object instanceof ProtoMessage) {
                            ProtoMessage aux = (ProtoMessage) object;
                            aux.setSerializer(out);
                        } else {
                            throw new RuntimeException("Error trying to serialize field: " + field.getName());
                        }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract ProtoMessage getNewEmptyInstance();

    private void readProtoMessage(ByteBuf in) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                switch (field.getType().getSimpleName()) {
                    case "byte":
                        out.writeByte(field.getByte(this));
                        break;
                    case "boolean":
                        out.writeBoolean(field.getBoolean(this));
                        break;
                    case "short":
                        out.writeShort(field.getByte(this));
                        break;
                    case "int":
                        out.writeInt(field.getInt(this));
                        break;
                    case "float":
                        out.writeFloat(field.getFloat(this));
                        break;
                    case "long":
                        out.writeLong(field.getLong(this));
                        break;
                    case "char":
                        out.writeChar(field.getChar(this));
                        break;
                    case "string":
                        out.writeCharSequence((String) field.get(this), Charset.defaultCharset());
                        break;
                    case "double":
                        out.writeDouble(field.getDouble(this));
                        break;
                    case "byte[]":
                        out.writeBytes((byte[]) field.get(this));
                        break;
                    default:
                        Object object = field.get(this);
                        if (object instanceof ProtoMessage) {
                            ProtoMessage aux = (ProtoMessage) object;
                            aux.setSerializer(out);
                        } else {
                            throw new RuntimeException("Error trying to serialize field: " + field.getName());
                        }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public ISerializer<ProtoMessage> serializer = new ISerializer<>() {
        @Override
        public void serialize(ProtoMessage protoMessage, ByteBuf out) throws IOException {
            protoMessage.setSerializer(out);
        }

        @Override
        public ProtoMessage deserialize(ByteBuf in) throws IOException {
            return null;
        }
    };
    
}
