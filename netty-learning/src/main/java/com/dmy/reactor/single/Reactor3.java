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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by DMY on 2018/9/17 17:09
 */
public class Reactor3 implements Runnable {

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    public Reactor3(int port) throws IOException {
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
                    new Handler3(selector, c).run();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Handler3 implements Runnable {
        private final SocketChannel socket;
        private final SelectionKey key;
        private ByteBuffer input = ByteBuffer.allocate(1024);
        private ByteBuffer output = ByteBuffer.allocate(1024);
        private static final int READING = 0, SENDING = 1, PROCESSING = 3;
        private int state = READING;
        ExecutorService pool = Executors.newCachedThreadPool();

        public Handler3(Selector selector, SocketChannel c) throws IOException {
            this.socket = c;
            this.socket.configureBlocking(false);
            key = socket.register(selector, SelectionKey.OP_READ);
            key.attach(this);
            selector.wakeup();
        }

        public void run() {
            try {
                if (state == READING)
                    read();
                else if (state == SENDING)
                    send();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * read操作使用线程池处理任务
         */
        private synchronized void read() throws IOException {
            socket.read(input);
            if (inputIsComplete()) {
                state = PROCESSING;
                pool.execute(new Processer());
            }
        }

        private void send() throws IOException {
            output.flip();
            socket.write(output);
            if (outputIsComplete()) {
                key.cancel();
            }
            state = READING;
            key.interestOps(SelectionKey.OP_READ);
        }

        private void process() {
            //读数据
            StringBuilder reader = new StringBuilder();
            input.flip();
            while (input.hasRemaining()) {
                reader.append((char) input.get());
            }
            System.out.println("[Client-INFO]");
            System.out.println(reader.toString());
            String str = "HTTP/1.1 200 OK\r\nDate: Fir, 10 March 2017 21:20:01 GMT\r\n" +
                    "Content-Type: text/html;charset=UTF-8\r\nContent-Length: 32\r\nConnection:close" +
                    "\r\n\r\nWelcome JAVA World " + System.currentTimeMillis();
            output.put(str.getBytes());
            System.out.println("process .... ");
        }

        private boolean inputIsComplete() {
            return input.hasRemaining();
        }

        private boolean outputIsComplete() {
            return output.hasRemaining();
        }

        private synchronized void processAndHandOff() {
            process();
            state = SENDING; // or rebind attachment
            key.interestOps(SelectionKey.OP_WRITE);
            System.out.println("processer over....");
        }

        class Processer implements Runnable {
            public void run() {
                processAndHandOff();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Reactor3(9001)).start();
        System.out.println("Server start...");
    }
}
