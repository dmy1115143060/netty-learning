package com.dmy.reactor.multiple;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

/**
 * Created by DMY on 2018/9/17 17:11
 */
public class Reactor extends Thread {

    private final Selector selector;

    /**
     * 主从的标志
     */
    private final boolean isMainReactor;

    public Selector getSelector() {
        return selector;
    }

    public Reactor(int port, boolean isMainReactor) throws IOException {
        this.isMainReactor = isMainReactor;
        selector = Selector.open();
        System.out.println(selector + " isMainReactor = " + isMainReactor);
        if (isMainReactor) {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            serverSocketChannel.configureBlocking(false);
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            selectionKey.attach(new Acceptor(selector, serverSocketChannel));
            selector.wakeup();
            System.out.println(getClass().getSimpleName() + " start on " + port + " ...\n");
        }
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                // 会阻塞导致不能register，设置阻塞时间
                int n = selector.select(10);
                if (n == 0)
                    continue;
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    dispatch(iterator.next());
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void dispatch(SelectionKey k) {
        Runnable runnable = (Runnable) k.attachment();
        if (runnable != null)
            runnable.run();
    }

}