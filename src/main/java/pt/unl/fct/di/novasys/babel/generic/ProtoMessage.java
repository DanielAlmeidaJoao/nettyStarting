package pt.unl.fct.di.novasys.babel.generic;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;

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

    private void writeString(ByteBuf out, String value){
        out.writeInt(value.length());
        out.writeCharSequence(value,Charset.defaultCharset());
    }
    private String readString(ByteBuf in){
        return in.readCharSequence(in.readInt(),Charset.defaultCharset()).toString();
    }

    private void serializeMessage(ByteBuf out) {
        System.out.println("IS GOING TO SERIALIZE NOW");
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            System.out.println(fields.length);
            for (Field field : fields) {

                if(Modifier.isFinal(field.getModifiers())||Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                field.setAccessible(true);
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
                    case "String":
                        writeString(out,(String) field.get(this));
                        break;
                    case "double":
                        out.writeDouble(field.getDouble(this));
                        break;
                    case "byte[]":
                        byte [] bytes = (byte[]) field.get(this);
                        out.writeInt(bytes.length);
                        out.writeBytes(bytes);
                        break;
                    default:
                        Object object = field.get(this);
                        if (object instanceof ProtoMessage) {
                            ProtoMessage aux = (ProtoMessage) object;
                            aux.serializeMessage(out);
                        }if (object instanceof Host) {
                            Host.serializer.serialize((Host) object,out);
                        } else {
                            throw new RuntimeException("Error trying to serialize field: " + field.getName());
                        }
                }
                System.out.println("SERIALIZING HOST: "+field.getType().getSimpleName());
            }
        } catch (Exception e) {
            System.out.println("ENDING ENDING ENDING MENDING!!!");
            throw new RuntimeException(e);
        }
        System.out.println("IS GOING TO SERIALIZE finished");
    }

    private void deserializeMessage(ByteBuf in) {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                if(Modifier.isFinal(field.getModifiers())||Modifier.isStatic(field.getModifiers())){
                    continue;
                }
                field.setAccessible(true);
                System.out.println("SIMPLE NAME: "+field.getType().getSimpleName());
                switch (field.getType().getSimpleName()) {
                    case "byte":
                        field.setByte(this,in.readByte());
                        break;
                    case "boolean":
                        field.setBoolean(this,in.readBoolean());
                        break;
                    case "short":
                        field.setShort(this,in.readShort());
                        break;
                    case "int":
                        field.setInt(this,in.readInt());
                        break;
                    case "float":
                        field.setFloat(this,in.readFloat());
                        break;
                    case "long":
                        field.setLong(this,in.readLong());
                        break;
                    case "char":
                        field.setChar(this,in.readChar());
                        break;
                    case "String":
                        field.set(this,readString(in));
                        break;
                    case "double":
                        field.setDouble(this,in.readDouble());
                        break;
                    case "byte[]":
                        field.set(this,in.readBytes(in.readInt()));
                        break;
                    default:
                        Object object = field.get(this);
                        if(object == null){
                            throw new RuntimeException(field.getName()+": getNewEmptyInstance() result can not have object children with null values!");
                        } else if (object instanceof ProtoMessage) {
                            ProtoMessage protoMessage = getNewEmptyInstance();
                            protoMessage.deserializeMessage(in);
                            field.set(this,protoMessage);
                        }if (object instanceof Host) {
                            field.set(this,Host.serializer.deserialize(in));
                        }else {
                            throw new RuntimeException("Error trying to deserialize field: " + field.getName());
                        }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract <V extends ProtoMessage> ProtoMessage getNewEmptyInstance();

    public static <V extends ProtoMessage> ISerializer<V> newSerializer(Class<V> zclass){
        V emptyMessage = null;
        try{
            for (Constructor<?> constructor : zclass.getConstructors()) {
                if(constructor.getParameterCount() == 0){
                    emptyMessage = (V) constructor.newInstance();
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if( emptyMessage == null ){
            throw new RuntimeException(zclass.getName() + " IS MISSING AN EMPTY CONSTRUCTOR!");
        }
        V finalEmptyMessage = emptyMessage;
        return new ISerializer<>() {
            @Override
            public void serialize(ProtoMessage protoMessage, ByteBuf out) throws IOException {
                System.out.println("GOING TO SERIALIZE MESSAGE "+protoMessage);
                protoMessage.serializeMessage(out);
            }

            @Override
            public V deserialize(ByteBuf in) throws IOException {
                System.out.println("DESSSE ASDFD SDFFSFDG GDFGDF");
                ProtoMessage protoMessage = finalEmptyMessage.getNewEmptyInstance();
                protoMessage.deserializeMessage(in);
                System.out.println("DESSSE FINALLLE");

                return (V) protoMessage;
            }
        };
    };

}
