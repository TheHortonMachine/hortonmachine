package org.hortonmachine.dbs.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;

/**
 * @author Antonello Andrea (www.hydrologis.com)
 */
public class SerializationUtilities {

    /**
     * Serialize an Object to disk.
     * 
     * @param obj
     *            the object to serialize.
     * @return the bytes.
     * @throws IOException
     */
    public static byte[] serialize( Object obj ) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            out.close();
            return bos.toByteArray();
        }
    }

    /**
    * Deserialize a byte array to a given object.
    * 
    * @param bytes
    *            the byte array.
    * @param adaptee
    *            the class to adapt to.
    * @return the object.
    * @throws Exception
    */
    public static <T> T deSerialize( byte[] bytes, Class<T> adaptee ) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object readObject = in.readObject();
        return adaptee.cast(readObject);
    }

    /**
     * Serialize an object to disk.
     * 
     * @param file
     *            the file to write to.
     * @param obj
     *            the object to write.
     * @throws IOException
     */
    public static void serializeToDisk( File file, Object obj ) throws IOException {
        byte[] serializedObj = serialize(obj);
        try (RandomAccessFile raFile = new RandomAccessFile(file, "rw")) {
            raFile.write(serializedObj);
        }
    }

    /**
     * Deserialize a file to a given object.
     * 
     * @param file
     *            the file to read.
     * @param adaptee
     *            the class to adapt to.
     * @return the object.
     * @throws Exception
     */
    public static <T> T deSerializeFromDisk( File file, Class<T> adaptee ) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            // System.out.println(length + "/" + (int) length);
            byte[] bytes = new byte[(int) length];
            int read = raf.read(bytes);
            if (read != length) {
                throw new IOException();
            }
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object readObject = in.readObject();
            return adaptee.cast(readObject);
        }
    }
}
