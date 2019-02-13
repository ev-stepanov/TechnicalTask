package interfaces;

import exceptions.CircularReference;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface SuperEncoder {
    byte[] serialize(Object anyBean) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, CircularReference;
    Object deserialize(byte[] data) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException, IOException, NoSuchMethodException, InvocationTargetException;
}
