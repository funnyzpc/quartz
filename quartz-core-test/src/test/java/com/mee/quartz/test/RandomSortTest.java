package com.mee.quartz.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * RandomSortTest
 *
 * @author shaoow
 * @version 1.0
 * @className RandomSortTest
 * @date 2024/8/23 15:01
 */
public class RandomSortTest {

        public static void main(String[] args) {
            // 创建一个ArrayList并添加一些元素
            List<Integer> numbers = new ArrayList<>();
            numbers.add(1);
            numbers.add(2);
            numbers.add(3);
            numbers.add(4);
            numbers.add(5);
            System.out.println("原始数据:"+numbers);
            IntStream.range(0,5).forEach(item->{
                Collections.shuffle(numbers);
                System.out.println("排序后数据:"+numbers);
            });

        }


}
