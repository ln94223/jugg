package com.lorabit.rpc.meta;

import com.lorabit.rpc.base.RpcConfig;
import com.lorabit.rpc.exception.RpcException;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author lorabit
 * @since 16-3-8
 *
 * Binary Packet Format:
 *
 * <pre>
 * int = 4 bytes
 * float = 4 bytes
 * bytes = variable length
 * field, state code
 *
 * ---------------------------------------------------------
 * MAGIC_CODE1(2 bytes)|  1
 * ---------------------------------------------------------
 * TOTAL(int)          |  1
 * ---------------------------------------------------------
 * CHECKSUM(long)      |  1
 * ---------------------------------------------------------
 * VERSION(float)      |  1
 * ---------------------------------------------------------
 * FLAG(int)           |  1
 * ---------------------------------------------------------
 * UUID(long)          |  1
 * ---------------------------------------------------------
 * CONFIG_SIZE(int)    |  3
 * ---------------------------------------------------------
 * CONFIG(bytes)       |  4
 * ---------------------------------------------------------
 * DOMAIN_SIZE(int)    |  5
 * ---------------------------------------------------------
 * DOMAIN(bytes)       |  6
 * ---------------------------------------------------------
 * METHOD_SIZE(int)    |  7
 * ---------------------------------------------------------
 * METHOD(bytes)       |  8
 * ---------------------------------------------------------
 * PARAMETER_SIZE(int) |  9
 * ---------------------------------------------------------
 * PARAMETER(bytes)    |  10
 * ---------------------------------------------------------
 * RETURN_SIZE(int)    |  11
 * ---------------------------------------------------------
 * RETURN(bytes)       |  12
 * ---------------------------------------------------------
 * EXCEPTION_SIZE(int) |  13
 * ---------------------------------------------------------
 * EXCEPTION(bytes)    |  14
 * ---------------------------------------------------------
 * </pre>
 */
public class BinaryPacketData {

  public final static byte[] EMPTY = {};
  public final static byte[] MAGIC_CODE = {11, 18};

  public float version;
  public int flag;
  public long uuid;
  public RpcConfig conf;
  public String domain;
  public String method;
  public Object[] param;
  public Object ret;
  public Throwable ex;


  public ByteBuf getBytes() {
    ByteBuf buff = Unpooled.buffer();
    byte[] bytes;
    buff.writeByte(MAGIC_CODE[0]);
    buff.writeByte(MAGIC_CODE[1]);

    int startWriterIndex = buff.writerIndex();
    buff.writeInt(0);  // take place for total
    buff.writeLong(0L); // tale place for checksum

    // version
    buff.writeFloat(version);

    // flag
    buff.writeInt(flag);

    // uuid
    buff.writeLong(uuid);

    // config
    try {
      bytes = ObjToByte(conf);
    } catch (RpcException e) {
      bytes = EMPTY;
      if (ex == null) {
        ex = e;
      }
    }
    buff.writeInt(bytes.length);
    buff.writeBytes(bytes);

    // domain
    if (domain == null) {
      bytes = EMPTY;
    } else {
      bytes = domain.getBytes();
    }
    buff.writeInt(bytes.length);
    buff.writeBytes(bytes);

    // method
    if (method == null) {
      bytes = EMPTY;
    } else {
      bytes = method.getBytes();
    }
    buff.writeInt(bytes.length);
    buff.writeBytes(bytes);

    // param
    try {
      bytes = ObjToByte(param);
    } catch (RpcException e) {
      bytes = EMPTY;
      if (ex == null) {
        ex = e;
      }
    }
    buff.writeInt(bytes.length);
    buff.writeBytes(bytes);

    // return
    try {
      bytes = ObjToByte(ret);
    } catch (RpcException e) {
      bytes = EMPTY;
      if (ex == null) {
        ex = e;
      }
    }
    buff.writeInt(bytes.length);
    buff.writeBytes(bytes);

    // exception
    bytes = errToBytes(ex);
    buff.writeInt(bytes.length);
    buff.writeBytes(bytes);

    // place in total and checksum
    int endWriteIndex = buff.writerIndex();
    buff.writerIndex(startWriterIndex);
    buff.writeInt(endWriteIndex);

    Checksum ck = new Adler32();
    // magic_code + total
    ck.update(buff.array(), startWriterIndex - 2, 6);
    // buffer.writerIndex(total_pos + 4);
    buff.writeLong(ck.getValue());


    return buff.writerIndex(endWriteIndex);
  }

  public byte[] ObjToByte(Object obj) throws RpcException {
    if (obj == null) {
      return EMPTY;
    }
    try {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(byteOut);
      oos.writeObject(obj);
      return byteOut.toByteArray();
    } catch (IOException e) {
      throw new RpcException(e);
    }
  }

  protected byte[] errToBytes(Throwable t) {
    if (t == null) {
      return EMPTY;
    }
    String ret = ExceptionUtils.getStackTrace(t);
    return ret.getBytes();
  }

}
