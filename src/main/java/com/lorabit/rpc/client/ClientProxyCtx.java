package com.lorabit.rpc.client;

import com.lorabit.rpc.base.RpcConfig;
import com.lorabit.rpc.exception.RpcException;
import com.lorabit.rpc.exception.RpcNoMoreConnException;
import com.lorabit.rpc.exception.RpcTimeoutException;
import com.lorabit.rpc.meta.BinaryPacketData;
import com.lorabit.rpc.meta.RpcRemoteLatch;
import com.lorabit.rpc.router.IORouter;
import com.lorabit.rpc.router.RpcIOPool;
import com.lorabit.rpc.router.RpcIOSession;
import com.lorabit.rpc.router.impl.ModIOBalance;

import org.mockito.cglib.proxy.Enhancer;
import org.mockito.cglib.proxy.MethodInterceptor;
import org.mockito.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lorabit
 * @since 16-3-7
 */
public class ClientProxyCtx<T> implements MethodInterceptor {
  private static final RpcIOPool pool = new RpcIOPool();

  private long timeout;
  private List<String> methodNames = new ArrayList<>();
  private String domainName;
  private IORouter router;

  private T serviceProxy;

  private ClientProxyCtx(Class clazz, String group, List<String> urls) {
    this.domainName = clazz.getName();
    this.router = ModIOBalance.getInstance(group, urls);
    for (Method method : clazz.getDeclaredMethods()) {
      methodNames.add(method.getName());
    }
  }


  public static <T> ClientProxyCtx<T> createFactoryCtx(
      Class clazz,
      List<String> urls,
      String group,
      long timeout) {
    ClientProxyCtx client = new ClientProxyCtx<>(clazz, group, urls);
    client.timeout = timeout;
    client.serviceProxy = (T) Enhancer.create(null, new Class[]{clazz}, client);
    return client;
  }

  public T getProxy() {
    return serviceProxy;
  }

  @Override
  public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
      throws Throwable {
    String name = method.getName();
    if (!methodNames.contains(name)) {
      throw new RpcException("no such proxy method " + name);
    }

    BinaryPacketData packet = new BinaryPacketData();
    RpcConfig config = new RpcConfig();
    config.addConf("timebase", System.currentTimeMillis());
    packet.conf = config;
    packet.domain = domainName;
    packet.param = args;
    packet.method = name;

    Object ret;
    String url = null;
    RpcRemoteLatch latch = new RpcRemoteLatch(timeout);
    RpcIOSession session = null;
    try {
      url = this.router.next(null);
      session = pool.getSession(url);  //will never return a active session so its thread safe
      packet.uuid = session.getUuid().incrementAndGet();
      latch.setUuid(packet.uuid);
      session.setLatch(latch);
      session.write(packet);
      ret = latch.getResult();
    } catch (Throwable e) {
      router.fail(url);
      if(e instanceof RpcTimeoutException){
        throw e;
      }
      if(e instanceof RpcNoMoreConnException){
        throw e;
      }
      throw  e;
    } finally {
      pool.releaseIOSession(session);
    }

    return ret;
  }
}
