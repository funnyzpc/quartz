package com.mee.quartz.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * AioClientTest
 *
 * @author shaoow
 * @version 1.0
 * @className AioClientTest
 * @date 2024/8/2 11:17
 */
public class AioClientTest {

    public static void main(String[] args) throws IOException {
        AsynchronousSocketChannel asc = AsynchronousSocketChannel.open();
        asc.connect(new InetSocketAddress("127.0.0.1",9000));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put("hello youth!".getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        asc.write(buffer);
        System.out.println("send msg success!");
//        ThreadPool
    }
}
