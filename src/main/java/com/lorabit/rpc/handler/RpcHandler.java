package com.lorabit.rpc.handler;

import com.lorabit.rpc.processor.RpcContext;

/**
 * @author lorabit
 * @since 16-3-8
 */
public interface RpcHandler {
   void lookUp(RpcContext ctx) throws Exception;

   void invoke(RpcContext ctx) throws Exception;
}
