import exceptions.CircularReference;
import interfaces.SuperEncoder;

import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class Connector implements SuperEncoder {
    private  Map<String, Integer> serializedClasses = new HashMap<>();
    private int positionInByteArray = 0;

    @Override
    public byte[] serialize(Object anyBean) throws IOException, IllegalAccessException, CircularReference {
        if (anyBean == null)
            return new byte[0];

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(stream);

        String nameClass = anyBean.getClass().getName();
        dos.writeUTF(nameClass);

        if (checkCircularReference(nameClass)) {
            throw new CircularReference();
        }

        serializedClasses.put(nameClass, 1);

        Field[] allClassFields = anyBean.getClass().getDeclaredFields();
        for (Field field : allClassFields) {
            field.setAccessible(true);
            dos.writeUTF(field.getName());
            String typeName = field.getType().getName();
            Object object = field.get(anyBean);
            if (object != null) {
                dos.writeUTF(typeName);
                writeFieldWithValue(dos, object);
            } else {
                dos.writeUTF("null");
            }
        }

        serializedClasses.put(nameClass, 2);
        dos.flush();
        return stream.toByteArray();
    }

    @Override
    public Object deserialize(byte[] data) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, IOException {
        if (data == null || data.length == 0)
            return null;

        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(stream);

        dis.skipBytes(positionInByteArray);

        String className = dis.readUTF();
        Class<?> clazz = Class.forName(className);
        Object newClass = clazz.newInstance();

        int countFieldInClasses  = newClass.getClass().getDeclaredFields().length;

        for (int i = 0; i < countFieldInClasses; i++) {
            String fieldName = dis.readUTF();
            String typeName = dis.readUTF();

            Field field = newClass.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            String fullTypeName = field.getAnnotatedType().getType().getTypeName();
            field.set(newClass,readFieldWithValue(data, stream, dis, typeName, newClass, field, fullTypeName));
        }

        getPositionInByteArray(stream);

        return newClass;
    }

    private Object readFieldWithValue (byte[] data, ByteArrayInputStream stream, DataInputStream dis, String nameType, Object newClass, Field field, String fullTypeName) throws IOException, IllegalAccessException, InstantiationException, NoSuchFieldException, ClassNotFoundException {
        try {
            switch (nameType) {
                case "byte":
                case "java.lang.Byte":
                    return dis.readByte();
                case "short":
                case "java.lang.Short":
                    return dis.readShort();
                case "int":
                case "java.lang.Integer":
                    return dis.readInt();
                case "long":
                case "java.lang.Long":
                    return dis.readLong();
                case "float":
                case "java.lang.Float":
                    return dis.readFloat();
                case "double":
                case "java.lang.Double":
                    return dis.readDouble();
                case "boolean":
                case "java.lang.Boolean":
                    return dis.readBoolean();
                case "java.lang.Character":
                    return dis.readChar();
                case "java.lang.String":
                    return  dis.readUTF();
                case "java.math.BigDecimal":
                    return new BigDecimal(dis.readUTF());
                case "java.time.Instant":
                    long epochSecond = dis.readLong();
                    long nanoSecond = dis.readLong();
                    return Instant.ofEpochSecond(epochSecond, nanoSecond);
                case "java.util.List":
                    return readList(data, stream, dis, newClass, field, fullTypeName);
                case "java.util.Set":
                    return readSet(data, stream, dis, newClass, field, fullTypeName);
                case "java.util.Map":
                    return readMap(data, stream, dis, newClass, field, fullTypeName);
                case "null":
                    return null;
                default:
                    getPositionInByteArray(stream);
                    int positionBeforeDeserialize = positionInByteArray;
                    Object obj =  deserialize(data);
                    dis.skip(positionInByteArray - positionBeforeDeserialize);
                    return  obj;
            }
        } catch (EOFException ex) {
            field.set(newClass, null);
        }
        return null;
    }

    private void writeFieldWithValue (DataOutputStream dos, Object anyBean) throws IOException, IllegalAccessException, CircularReference {
        if(anyBean instanceof Byte)
            dos.writeByte((byte) anyBean);
        else if (anyBean instanceof Short)
            dos.writeShort((short) anyBean);
        else if (anyBean instanceof Integer)
            dos.writeInt((int) anyBean);
        else if (anyBean instanceof Long)
            dos.writeLong((long) anyBean);
        else if (anyBean instanceof Float)
            dos.writeFloat((float) anyBean);
        else if (anyBean instanceof Double)
            dos.writeDouble((double) anyBean);
        else if (anyBean instanceof Instant) {
            Instant instant = (Instant) anyBean;
            writeFieldWithValue(dos, instant.getEpochSecond());
            writeFieldWithValue(dos, (long)instant.getNano());
        }
        else if (anyBean instanceof  Character)
            dos.writeChar((Character) anyBean);
        else if (anyBean instanceof String)
            dos.writeUTF((String) anyBean);
        else if (anyBean instanceof BigDecimal)
            dos.writeUTF(anyBean.toString());
        else if (anyBean instanceof Boolean)
            dos.writeBoolean((Boolean) anyBean);
        else if (anyBean instanceof List)
            writeList(dos, anyBean);
        else if (anyBean instanceof Set)
            writeSet(dos, anyBean);
        else if (anyBean instanceof Map)
            writeMap(dos, anyBean);
        else
            dos.write(serialize(anyBean));
    }

    private Object readMap(byte[] data, ByteArrayInputStream stream, DataInputStream dis, Object newClass, Field field, String fullTypeName) throws IOException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InstantiationException {

        String nameClass = dis.readUTF();
        int arraySize = dis.readInt();

        String [] typeName = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.lastIndexOf('>')).split(", ");

        String typeKey = typeName[0].indexOf('<') != -1 ? typeName[0].substring(0, typeName[0].indexOf('<')) : typeName[0];
        String typeValue = typeName[1].indexOf('<') != -1 ? typeName[1].substring(0, typeName[1].indexOf('<')) : typeName[1];

        Map<Object, Object> map = createLinkToMap(nameClass);
        for (int i = 0; i < arraySize; i++) {
            Object key = readFieldWithValue(data, stream, dis, typeKey, newClass, field, typeName[0]);
            Object value = readFieldWithValue(data, stream, dis, typeValue, newClass, field, typeName[1]);
            map.put(key, value);
        }
        return map;
    }

    private Object readSet(byte[] data,ByteArrayInputStream stream, DataInputStream dis, Object newClass, Field field, String fullTypeName) throws IOException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InstantiationException {

        String nameClass = dis.readUTF();
        int arraySize = dis.readInt();

        String typeName = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.indexOf('>'));
        String typeValue = typeName.indexOf('<') != -1 ? typeName.substring(0, typeName.indexOf('<')) : typeName;

        Set<Object> set = createLinkToSet(nameClass);
        for (int i = 0; i < arraySize; i++) {
            set.add(readFieldWithValue(data, stream, dis, typeValue, newClass, field, typeName));
        }

        return set;
    }

    private Object readList(byte[] data,ByteArrayInputStream stream, DataInputStream dis, Object newClass, Field field, String fullTypeName) throws IOException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InstantiationException {

        String nameClass = dis.readUTF();
        int arraySize = dis.readInt();

        String typeName = fullTypeName.substring(fullTypeName.indexOf('<') + 1, fullTypeName.indexOf('>'));
        String typeValue = typeName.indexOf('<') != -1 ? typeName.substring(0, typeName.indexOf('<')) : typeName;

        List<Object> list = createLinkToList(nameClass);
        for (int i = 0; i < arraySize; i++) {
            list.add(readFieldWithValue(data, stream, dis, typeValue, newClass, field, typeName));
        }
        return list;
    }

    private List<Object> createLinkToList(String nameClass) {
        switch (nameClass) {
            case "java.util.ArrayList":
                return new ArrayList<>();
            case "java.util.LinkedList":
                return new LinkedList<>();
            case "java.util.Stack":
                return new Stack<>();
            case "java.util.Vector":
                return new Vector<>();
        }
        return null;
    }

    private Set<Object> createLinkToSet(String nameClass) {
        switch (nameClass) {
            case "java.util.LinkedHashSet":
                return new LinkedHashSet<>();
            case "java.util.HashSet":
                return new HashSet<>();
            case "java.util.TreeSet":
                return new TreeSet<>();
        }
        return null;
    }

    private Map<Object, Object> createLinkToMap(String nameClass) {
        switch (nameClass) {
            case "java.util.LinkedHashMap":
                return new LinkedHashMap<>();
            case "java.util.TreeMap":
                return new TreeMap<>();
            case "java.util.HashMap":
                return new HashMap<>();
            case "java.util.Hashtable":
                return new Hashtable<>();
        }
        return null;
    }

    private void writeMap (DataOutputStream dos, Object obj) throws IOException, IllegalAccessException, CircularReference {
        Map<Object, Object> map = (Map<Object, Object>)obj;

        dos.writeUTF(obj.getClass().getName());
        dos.writeInt(map.size());

        for(Map.Entry<Object, Object> item : map.entrySet()) {
            writeFieldWithValue(dos, item.getKey());
            writeFieldWithValue(dos, item.getValue());
        }
    }

    private void writeSet (DataOutputStream dos, Object obj) throws IOException, IllegalAccessException, CircularReference {
        Set<Object> set = (Set<Object>)obj;

        dos.writeUTF(obj.getClass().getName());
        dos.writeInt(set.size());

        for (Object value:set)
            writeFieldWithValue(dos, value);
    }

    private void writeList (DataOutputStream dos, Object obj) throws IOException, IllegalAccessException, CircularReference {
        List<Object> list = (List<Object>)obj;

        dos.writeUTF(obj.getClass().getName());
        dos.writeInt(list.size());

        for (Object value:list)
            writeFieldWithValue(dos, value);
    }

    private boolean checkCircularReference (String nameClass) throws  CircularReference{
        if (serializedClasses.containsKey(nameClass) && serializedClasses.get(nameClass) == 1) {
            throw new CircularReference();
        }
        return false;
    }

    private void getPositionInByteArray(ByteArrayInputStream stream) throws NoSuchFieldException, IllegalAccessException {
        Field pos = stream.getClass().getDeclaredField("pos");
        pos.setAccessible(true);
        positionInByteArray = (Integer)pos.get(stream);
    }
}