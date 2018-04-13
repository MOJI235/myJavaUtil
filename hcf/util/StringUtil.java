package com.hiyo.hcf.util;

import org.dswf.util.S;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    /**
     * 判断是不是数字
     * @param str
     * @return
     */
    public static boolean isNumeric(String str){
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        if( !isNum.matches() ){
            return false;
        }
        return true;
    }
    /**
     * 锁对象，可以为任意对象
     */
    private final static Object lockObj = "lockerOrder";
    /**
     * 订单号生成计数器
     */
    private static long orderNumCount = 0L;
    /**
     * 每毫秒生成订单号数量最大值
     */
    private int maxPerMSECSize = 1000;

    /**
     * 生成非重复订单号，理论上限1毫秒1000个，可扩展
     */
    public String genId() {
        String finOrderNum = "";
        try {
            // 最终生成的订单号
            synchronized (lockObj) {
                // 取系统当前时间作为订单号变量前半部分，精确到毫秒
                long nowLong = Long.parseLong(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
                // 计数器到最大值归零，可扩展更大，目前1毫秒处理峰值1000个，1秒100万
                if (orderNumCount > maxPerMSECSize) {
                    orderNumCount = 0L;
                }
                //组装订单号
                String countStr = maxPerMSECSize + orderNumCount + "";
                finOrderNum = nowLong + countStr.substring(1);
                orderNumCount++;

                // Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finOrderNum;
    }


    /**
     * 判断是不是手机号
     *
     * @param mobile
     * @return
     */
    public static boolean isMobile(String mobile) {
        Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(17[0-9])|(18[0,2-9]))\\d{8}$");
        Matcher m = p.matcher(mobile);
        return m.matches();
    }

    /**
     * 获取文件类型
     *
     * @param fileName
     * @return
     */
    public static String getFileType(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return "";
        } else {
            return fileName.substring(index + 1, fileName.length()).toLowerCase();
        }
    }

    public static String getFileName(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index <= 0) {
            return fileName;
        } else {
            return fileName.substring(0, index);
        }
    }


    /**
     * 获取UUID
     *
     * @return
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


    public static List<Integer> getRandom(int end, int count) {
        List<Integer> numList = new ArrayList<Integer>();
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            int num = r.nextInt(end);
            if (!numList.contains(num)) {
                numList.add(num);
            } else {
                i--;
            }
        }
        return numList;

    }

    public static String[] chars = new String[]{"a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z"};


    public static String generateShortUuid() {
        StringBuffer shortBuffer = new StringBuffer();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            shortBuffer.append(chars[x % 0x3E]);
        }
        return shortBuffer.toString();

    }


    /**
     * 获取request里面的参数
     *
     * @param request
     * @return
     */
    public static String getRequestParam(HttpServletRequest request) {
        Enumeration em = request.getParameterNames();
        String info = "";
        //接收方法
        info += "请求为:{" + request.getRequestURI() + "}--{" + request.getMethod() + "}";
        String param = "";
        while (em.hasMoreElements()) {
            String name = (String) em.nextElement();
            String value = request.getParameter(name);
            param += name + "---->" + value + ",";
        }
        if (param.length() > 0) {
            info += "接收的参数为:{" + param.substring(0, param.length() - 1) + "}";
        }
        return info;
    }

    public static HashMap getParams(String params) {
        if (S.isEmpty(params)) return null;
        HashMap map = new HashMap();
        String[] strArr = params.split("\\|");
        for (String strs : strArr) {
            String paramArr[] = strs.split("\\-");
            map.put(paramArr[0], paramArr[1]);
        }
        return map;
    }

    public static void main(String[] args) {
//		HashMap map = getParams("a-1&bb=2");
//		System.out.println(getUUID());
//		System.out.println(map.get("a").toString()+map.get("b"));
        for (int i = 0; i < 100; i++) {
            System.out.println(getRandom(10, 1));
        }
    }
}
