package com.dmy.reactor.multiple;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by DMY on 2018/9/17 17:10
 */
public class Server {
    /**
     * 主要负责接收客户端请求
     */
    private static Reactor mainReactor;

    /**
     * 负责处理IO操作
     */
    private static Reactor[] subReactors;
    private static final int SUB_SIZE = 3;
    private static final int port = 1234;

    private static AtomicInteger nextIndex = new AtomicInteger();

    /**
     * 轮询获得下一个subReactor来处理IO事件
     */
    public static Reactor nextSubReactor() {
        long nextIndexValue = nextIndex.getAndIncrement();
        if (nextIndexValue < 0) {
            nextIndex.set(0);
            nextIndexValue = 0;
        }
        return subReactors[(int) (nextIndexValue % subReactors.length)];
    }

    public static void main(String[] args) {
        try {
            mainReactor = new Reactor(port, true);
            subReactors = new Reactor[SUB_SIZE];
            for (int i = 0; i < subReactors.length; i++) {
                subReactors[i] = new Reactor(port, false);
            }
            mainReactor.start();
            for (int i = 0; i < subReactors.length; i++) {
                subReactors[i].start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}