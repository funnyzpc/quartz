package org.quartz.core;

import org.junit.Test;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.StdJDBCConstants;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * StdJDBCConstantsTest
 *
 * @author shaoow
 * @version 1.0
 * @className StdJDBCConstantsTest
 * @date 2024/6/25 9:58
 */
public class StdJDBCConstantsTest {

    @Test
    public void test02(){
        String s = Integer.toBinaryString(2147483647);
        System.out.println(s);
        System.out.println(Integer.toBinaryString(Integer.MAX_VALUE));
    }

    ByteBuffer buffer = ByteBuffer.allocate(1024);
//    ByteBuffer out = ByteBuffer.allocate(1024);

    @Test
    public void test03() throws Exception {
        List<SocketChannel> scs = new ArrayList<SocketChannel>(8);
        ServerSocketChannel ssc = ServerSocketChannel.open();
        Selector selector = Selector.open();
        ssc.bind( new InetSocketAddress(8000));
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        for(;;){
            SocketChannel sc= ssc.accept();
            if(sc==null){
                System.out.println("此时没有客户连接，waiting...");
                TimeUnit.SECONDS.sleep(3);
            }else{
                System.out.println("有客户连接上来了...");
                scs.add(sc);
            }
            TAG:
            for(SocketChannel sct:scs){
                sct.configureBlocking(false);

                int effective = sct.read(buffer);
                if(effective!=0){
                    buffer.flip();
                    System.out.println(new java.lang.String(buffer.array()));
                    sct.close();
//                    sct.write("hello".getBytes(StandardCharsets.UTF_8))
                    sct.shutdownOutput();
                    scs.remove(sct);
                    break TAG;
                }else{
                    System.out.println("Client data is empty!");
                }
            }

        }
    }


}
