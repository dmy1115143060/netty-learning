package com.dmy.reactor.single;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by DMY on 2018/9/17 16:38
 */
public class Reactor1 implements Runnable {
    /**
     * 通道选择器
     */
    final Selector selector;

    /**
     * 接收客户端连接的Channel
     */
    final ServerSocketChannel serverSocketChannel;

    public Reactor1(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // 将给定的对象附加到此键。
        key.attach(new Acceptor());
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                // 等待事件的到来，若无事件，则此时会阻塞
                int n = selector.select();
                if (n == 0)
                    continue;
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeySet.iterator();
                // 处理所有Channel上面监听到的事件
                while (iterator.hasNext()) {
                    // 使用dispatch分发事件
                    dispatch(iterator.next());
                }
                selectionKeySet.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 分发事件，将事件分发到对应的Handler中
     */
    private void dispatch(SelectionKey k) {
        // 获取当前的附加对象，实际上获取的是Acceptor实例对象
        Runnable runnable = (Runnable) k.attachment();
        if (runnable != null)
            runnable.run();
    }

    class Acceptor implements Runnable {
        public void run() {
            try {
                // 获取客户端的连接请求,并获得对应的连接通道ScoketChannel
                SocketChannel c = serverSocketChannel.accept();
                if (c != null) {
                    System.out.println("New Connection ...");
                    new Handler(selector, c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理事件的Handler类
     */
    final class Handler implements Runnable {
        final SocketChannel socketChannel;
        final SelectionKey selectionKey;
        ByteBuffer input = ByteBuffer.allocate(1024);
        ByteBuffer output = ByteBuffer.allocate(1024);
        static final int READING = 0, SENDING = 1;
        int state = READING;

        public Handler(Selector selector, SocketChannel socketChannel) throws IOException {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            // 向Selector注册Channel
            selectionKey = socketChannel.register(selector, 0);
            selectionKey.attach(this);
            // 注册可读事件并唤醒selector
            selectionKey.interestOps(SelectionKey.OP_READ);
            selector.wakeup();
        }

        public void run() {
            try {
                if (state == READING)
                    read();
                else if (state == SENDING)
                    send();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        boolean inputIsComplete() {
            return input.hasRemaining();
        }

        boolean outputIsComplete() {
            return output.hasRemaining();
        }

        void process() {
            //读数据
            StringBuilder reader = new StringBuilder();
            input.flip();
            while (input.hasRemaining()) {
                reader.append((char) input.get());
            }
            System.out.println("[Client-INFO]");
            System.out.println(reader.toString());
            String str = "HTTP/1.1 200 OK\r\nDate: Fir, 10 March 2017 21:20:01 GMT\r\n" +
                    "Content-Type: text/html;charset=UTF-8\r\nContent-Length: 23\r\nConnection:close" +
                    "\r\n\r\nHelloWorld" + System.currentTimeMillis();
            output.put(str.getBytes());
            System.out.println("process over.... ");
        }

        void read() throws IOException {
            socketChannel.read(input);
            if (inputIsComplete()) {
                process();
                state = SENDING;
                selectionKey.interestOps(SelectionKey.OP_WRITE);
            }
        }

        void send() throws IOException {
            output.flip();
            socketChannel.write(output);
            if (outputIsComplete()) {
                selectionKey.cancel();
            }
            state = READING;
            selectionKey.interestOps(SelectionKey.OP_READ);
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Reactor1(9001)).start();
        System.out.println("Server start...");
    }
}
