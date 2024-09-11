package org.quartz.utils;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * RandomTest
 *
 * @author shaoow
 * @version 1.0
 * @className RandomTest
 * @date 2024/7/22 16:26
 */
public class RandomTest {


    public static void main(String[] args) {
        long t = System.currentTimeMillis();
        System.out.println("t="+t);
        Random random = new Random(t);
        IntStream.range(0,100).forEach(i->{
            int val = random.nextInt(7 * 1000);
            System.out.println(val);
        });

        List<String> lst = new ArrayList<String>(){{
            add("aa");
            add("bb");
            add("cc");
            add("dd");
            add("ee");
        }};
        Collections.shuffle(lst);
        for(String l:lst){
            System.out.println(l);
        }

    }

}
