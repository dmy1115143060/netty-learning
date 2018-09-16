package com.dmy.netty.echo.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by DMY on 2018/9/16 17:03
 */
public class SimpleDiscardHandler extends SimpleChannelInboundHandler<Object> {
    /**
     * 不需要显示的进行资源释放，SimpleChannelInboundHandler自动完成
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {

    }
}
