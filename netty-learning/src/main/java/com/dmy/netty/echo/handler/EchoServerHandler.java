package com.dmy.netty.echo.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * Created by DMY on 2018/9/12 16:47
 */
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 当Channel已经注册到它的EventLoop并且能够处理IO时被调用
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel注册成功：" + ctx.channel() + ", " + ctx.channel().eventLoop());
    }

    /**
     * 当Channel处于活动状态被调用；Channel已经连接/绑定并且已经就绪
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel处于活动状态！");
    }

    /**
     * 对于每个客户端传入的消息都需要调用该方法进行处理：业务处理逻辑实现对接收到的消息进行输出并回传给客户端
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("EchoServer received: " + in.toString(CharsetUtil.UTF_8));
        // write方法是异步的，channelRead方法返回后write方法仍然可能未完成，因此不能直接释放ByteBuf内存
        // 这也是为什么客户端和服务器使用不同的Handler原因
        ctx.write(in);
        // 记录方法调用，并将事件传递给下一个ChannelHandler
        ctx.fireChannelRead(msg);
    }

    /**
     * 通知ChannelInboundHandler最后一次对channelRead()的调用是当前批量读取中的最后一条消息
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 在读取操作期间，有异常抛出时会调用该方法
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
