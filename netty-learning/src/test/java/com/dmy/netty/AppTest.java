package com.dmy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;



/**
 * Unit test for simple App.
 */
public class AppTest {

    public static final int a = 5;

    @Test
    public void display() {
        ByteBuf buf = Unpooled.copiedBuffer("Hello".getBytes());
        while (buf.isReadable()) {
            System.out.println(buf.readByte());
        }
        System.out.println(buf.readerIndex());
        System.out.println(buf.writerIndex());
        buf.discardReadBytes();
        System.out.println(buf.readerIndex());
        System.out.println(buf.writerIndex());
        buf.writeByte(100);
        System.out.println(buf.writerIndex());

    }
}
