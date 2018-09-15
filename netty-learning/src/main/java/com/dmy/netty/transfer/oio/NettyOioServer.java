package com.dmy.netty.transfer.oio;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Created by DMY on 2018/9/14 15:24
 * Netty阻塞版本Server
 */
public class NettyOioServer {

    public void server(int port) throws Exception {
        final ByteBuf byteBuf = Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer("Hi\r\n", Charset.forName("UTF-8")));
        // 使用OioEventLoopGroup以允许阻塞模式（兼容旧的IO）
        EventLoopGroup eventLoopGroup = new OioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(eventLoopGroup)
                    .channel(OioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel socketChannel) {
                            // 添加一个ChannelInboundHandlerAdapter以拦截和处理事件
                            socketChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    // 将消息写到客户端，并添加ChannelFutureListener，以便消息一被写完就关闭连接
                                    ctx.writeAndFlush(byteBuf.duplicate()).addListener(ChannelFutureListener.CLOSE);
                                }
                            });
                        }
                    });
            // 绑定服务器接收连接
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
        } finally {
            eventLoopGroup.shutdownGracefully().sync();
        }
    }
}
