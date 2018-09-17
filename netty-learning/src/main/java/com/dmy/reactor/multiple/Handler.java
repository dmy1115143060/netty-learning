package com.dmy.reactor.multiple;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Created by DMY on 2018/9/17 17:12
 */
public class Handler implements Runnable {
    private final SocketChannel socketChannel;
    private final SelectionKey selectionKey;
    private ByteBuffer input = ByteBuffer.allocate(1024);
    private ByteBuffer output = ByteBuffer.allocate(1024);
    private static final int READING = 0, SENDING = 1;
    private int state = READING;

    public Handler(Selector selector, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        selectionKey.attach(this);
        selector.wakeup();
        System.out.println(selector + " connect success...");
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

    private boolean inputIsComplete() {
        return input.hasRemaining();
    }

    private boolean outputIsComplete() {
        return output.hasRemaining();
    }

    private void process() {
        // 读数据
        StringBuilder reader = new StringBuilder();
        input.flip();
        while (input.hasRemaining()) {
            reader.append((char) input.get());
        }
        System.out.println("[Client-INFO]");
        System.out.println(reader.toString());
        String str = "HTTP/1.1 200 OK\r\nDate: Fir, 10 March 2017 21:20:01 GMT\r\n" +
                "Content-Type: text/html;charset=UTF-8\r\nContent-Length: 24\r\nConnection:close" +
                "\r\n\r\nHelloRector" + System.currentTimeMillis();
        output.put(str.getBytes());
        System.out.println("process .... ");
    }

    private void read() throws IOException {
        socketChannel.read(input);
        if (inputIsComplete()) {
            process();
            state = SENDING;
            selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void send() throws IOException {
        output.flip();
        socketChannel.write(output);
        if (outputIsComplete()) {
            selectionKey.cancel();
        }
        state = READING;
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

}
