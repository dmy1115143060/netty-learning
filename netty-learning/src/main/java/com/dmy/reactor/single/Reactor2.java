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
 * Created by DMY on 2018/9/17 17:06
 */
public class Reactor2 implements Runnable {

    final Selector selector;
    final ServerSocketChannel serverSocketChannel;

    public Reactor2(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        SelectionKey key = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(new Acceptor());
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                // 等待事件
                int n = selector.select();
                if (n == 0)
                    continue;
                Set<SelectionKey> set = selector.selectedKeys();
                Iterator<SelectionKey> iterator = set.iterator();
                while (iterator.hasNext()) {
                    dispatch(iterator.next());
                }
                set.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey k) {
        Runnable runnable = (Runnable) k.attachment();
        if (runnable != null)
            runnable.run();
    }

    class Acceptor implements Runnable {
        public void run() {
            try {
                SocketChannel c = serverSocketChannel.accept();
                if (c != null) {
                    System.out.println("New Connection ...");
                    new Handler2(selector, c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Handler2 implements Runnable {

        final SocketChannel socket;
        final SelectionKey key;
        ByteBuffer input = ByteBuffer.allocate(1024);
        ByteBuffer output = ByteBuffer.allocate(1024);
        static final int READING = 0, SENDING = 1;
        int state = READING;

        public Handler2(Selector selector, SocketChannel c) throws IOException {
            this.socket = c;
            this.socket.configureBlocking(false);
            key = socket.register(selector, SelectionKey.OP_READ);
            key.attach(this);
            selector.wakeup();
        }

        public void run() {
            try {
                socket.read(input);
                if (inputIsComplete()) {
                    process();
                    key.attach(new Sender());//send thread
                    key.interestOps(SelectionKey.OP_WRITE);
                    key.selector().wakeup();
                }
            } catch (IOException e) {
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
            String str = "HTTP/1.1 200 OK\r\nDate: Sat, 31 Dec 2005 23:59:59 GMT\r\n" +
                    "Content-Type: text/html;charset=UTF-8\r\nContent-Length: 23\r\nConnection:close" +
                    "\r\n\r\nWelcome NIO " + System.currentTimeMillis();
            output.put(str.getBytes());
            System.out.println("process over.... ");
        }

        class Sender implements Runnable {

            public void run() {
                try {
                    output.flip();
                    socket.write(output);
                    if (outputIsComplete())
                        key.cancel();
                    socket.close();//记住要关闭
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Reactor2(9001)).start();
        System.out.println("Server start...");
    }
}
