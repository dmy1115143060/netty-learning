package com.dmy.netty.echo.run;

import com.dmy.netty.echo.handler.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;


/**
 * Created by DMY on 2018/9/12 17:03
 */
public class EchoServer {

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        new EchoServer(8888).start();
    }

    public void start() throws Exception {
        final EchoServerHandler serverHandler = new EchoServerHandler();
        // 对事件进行处理：接受新连接，读/写数据
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    // 指定所使用的NIO传输Channel
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    // 当一个新的连接被接受时，一个新的子Channel将会被创建，而ChannelInitializer将会把自己创建的
                    // ChannelHandler实例对象添加到该Channel的ChannelPipeline中
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline().addLast(serverHandler);
                }
            });
            // 异步地绑定服务器；调用sync()方法阻塞等待直到绑定完成
            ChannelFuture future = b.bind().sync();
            // 获取Channel的CloseFuture，并且阻塞当前线程直到它完成
            future.channel().closeFuture().sync();
        } finally {
            // 释放资源
            group.shutdownGracefully().sync();
        }
    }
}
