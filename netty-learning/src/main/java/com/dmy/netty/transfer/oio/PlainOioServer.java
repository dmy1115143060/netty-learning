package com.dmy.netty.transfer.oio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by DMY on 2018/9/14 11:08
 * 利用阻塞IO实现连接
 */
public class PlainOioServer {

    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            4,
            8,
            5,
             TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>());

    public void server(int port) throws Exception {
        final ServerSocket serverSocket = new ServerSocket(port);
        try {
            for (; ;) {
                final Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                threadPool.execute(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OutputStream outputStream;
                        try {
                            outputStream = clientSocket.getOutputStream();
                            outputStream.write("Hi\r\n".getBytes(Charset.forName("UTF-8")));
                            outputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                clientSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
