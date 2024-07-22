package org.quartz.core;

import org.junit.Test;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.StdJDBCConstants;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.simpl.SimpleClassLoadHelper;
import org.slf4j.LoggerFactory;

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
    public void test01() throws NoSuchDelegateException {
        StdJDBCDelegate stdJDBCDelegate = new StdJDBCDelegate();
        stdJDBCDelegate.initialize(LoggerFactory.getLogger(getClass()), "QRTZ_", "MEE_QUARTZ", "INSTANCE", new SimpleClassLoadHelper(), false, "");
        String sql = StdJDBCConstants.DELETE_FIRED_TRIGGER;
        System.out.println(sql);
        String rtp = stdJDBCDelegate.rtp(sql);
        System.out.println(rtp);
    }

}
