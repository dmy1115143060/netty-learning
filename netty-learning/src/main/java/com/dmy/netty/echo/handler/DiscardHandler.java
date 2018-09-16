package com.dmy.netty.echo.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by DMY on 2018/9/16 16:58
 */
@ChannelHandler.Sharable
public class DiscardHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 显示地释放与池化的ByteBuf实例相关的内存：丢弃已接收的消息
        ReferenceCountUtil.release(msg);
    }
}
