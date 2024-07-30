package com.mee.quartz.test;

import com.mee.quartz.job.Simple01TestService;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.utils.ClassUtils;

/**
 * DisallowConcurrentExecutionTest
 *
 * @author shaoow
 * @version 1.0
 * @className DisallowConcurrentExecutionTest
 * @date 2024/7/24 10:16
 */
public class DisallowConcurrentExecutionTest {

    @Test
    public void test01(){
        Class<Simple01TestService> clazz = Simple01TestService.class;
        boolean annotationPresent = ClassUtils.isAnnotationPresent(clazz, DisallowConcurrentExecution.class);
        System.out.println(annotationPresent);

    }
}
