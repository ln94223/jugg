package com.lorabit.rpc.router;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;

/**
 * @author lorabit
 * @since 16-3-8
 */
public class RpcHandlerInitializer extends ChannelInitializer<SocketChannel> {

  SimpleChannelInboundHandler handler;

  public RpcHandlerInitializer() {
  }

  public RpcHandlerInitializer(SimpleChannelInboundHandler handler) {
    this.handler = handler;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("frame", new RpcPacketDecoder());
    pipeline.addLast("handler", handler);
  }
}
