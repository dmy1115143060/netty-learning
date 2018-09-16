package com.dmy.netty.transfer.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by DMY on 2018/9/14 11:29
 * 利用非阻塞IO实现连接
 */
public class PlainNioServer {

    public void server(int port) throws Exception {

        // 创建一个Channel，用于接收客户端的连接
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        ServerSocket serverSocket = serverSocketChannel.socket();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
        serverSocket.bind(inetSocketAddress);

        // 打开一个Selector选择器，并将创建的Channel注册到该Selector上，监听事件连接事件
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        final ByteBuffer byteBuffer = ByteBuffer.wrap("Hi\r\n".getBytes(Charset.forName("UTF-8")));
        for (; ; ) {
            try {
                // 等待需要处理的新事件，阻塞将一直持续到下一个传入事件
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            // 获取所有事件对应的SelectionKey
            Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
            while (selectionKeyIterator.hasNext()) {
                SelectionKey selectionKey = selectionKeyIterator.next();
                selectionKeyIterator.remove();
                try {
                    // 接收客户端并为其创建一个Channel，并将它注册到Selector上
                    if (selectionKey.isAcceptable()) {
                        ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
                        SocketChannel clientChannel = serverChannel.accept();
                        clientChannel.configureBlocking(false);
                        clientChannel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, byteBuffer.duplicate());
                        System.out.println("Accepted connection from " + clientChannel);
                    }

                    // 检查套接字是否准备好写数据
                    if (selectionKey.isWritable()) {
                        SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                        ByteBuffer buffer = (ByteBuffer) selectionKey.attachment();
                        while (buffer.hasRemaining()) {
                            // 将数据写入到已连接的客户端上
                            if (clientChannel.write(buffer) == 0) {
                                break;
                            }
                        }
                        clientChannel.close();
                    }
                } catch (IOException e) {
                    selectionKey.cancel();
                    try {
                        selectionKey.channel().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
