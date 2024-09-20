package org.quartz.simpl;

import java.net.InetAddress;

/**
 * SystemPropGenerator
 *
 * @author shaoow
 * @version 1.0
 * @className SystemPropGenerator
 * @date 2024/9/2 16:21
 */
public final class SystemPropGenerator {

//    private static InetAddress INET_ADDRESS =null;
//    // todo ... 需改造: 在获取不到主机信息时必要有默认参数
//    static {
//        try {
//            INET_ADDRESS = InetAddress.getLocalHost();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//    }
    /**
     * 默认序列
     */
    private static final String SEQ = SeqGenUtil.genSeq();
    private static String HOST_IP = null ;
    private static String HOST_NAME = null;
    static{
        try {
            HOST_IP = InetAddress.getLocalHost().getHostAddress();
            HOST_NAME = InetAddress.getLocalHost().getHostName();
        }catch (Exception e){
            System.out.println("未能获取到主机信息:HOST_IP,HOST_NAME");
        }
    }

    public static String hostIP(){
       return null==HOST_IP||"".equals(HOST_IP)?"NI_"+SEQ:HOST_IP;
//       return "NI_"+SEQ;
    }

    public static String hostName(){
        return null==HOST_NAME||"".equals(HOST_NAME)?"NN_"+SEQ:HOST_NAME;
    }

}
