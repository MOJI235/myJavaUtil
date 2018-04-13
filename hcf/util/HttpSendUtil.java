package com.hiyo.hcf.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

public class HttpSendUtil {
    /**
     * 向指定URL发送GET方法的请求
     * 
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setRequestProperty("contentType", "UTF-8");
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     * 
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 获取参数字符串
     * @param map
     * @return
     */
    public static String getParamStr(HashMap map){
        String param = "";
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            try {
                param += entry.getKey()+"="+ URLEncoder.encode(entry.getValue().toString(),"UTF-8")+"&";
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        param=param.substring(0,param.length()-1);
        return  param;
    }

    /**
     * 开放平台加密参数拼接
     * @param map
     * @return
     */
    public static String getParamStrWithoutEncoder(HashMap map){
        Set keySet = map.keySet();
        Object[] keyArray = keySet.toArray();
        Arrays.sort(keyArray);
        String signStr="";
        for(Object obj:keyArray){
            if(map.get(obj)!=null&&!"".equals(map.get(obj))&&!"null".equals(map.get(obj))){
                signStr+=obj.toString()+"="+map.get(obj)+"&";
            }
        }
        signStr=signStr.substring(0,signStr.length()-1);
//        String param = "";
//        Iterator iter = map.entrySet().iterator();
//        while (iter.hasNext()) {
//            Map.Entry entry = (Map.Entry) iter.next();
//            param += entry.getKey()+"="+entry.getValue()+"&";
//        }
//        param=param.substring(0,param.length()-1);
        return  signStr;
    }

    /**
     * 参数拼接不带连接符
     * @param map
     * @return
     */
    public static String getParamStrWithoutAnd(HashMap map){
        Set keySet = map.keySet();
        Object[] keyArray = keySet.toArray();
        Arrays.sort(keyArray);
        String signStr="";
        for(Object obj:keyArray){
            if(map.get(obj)!=null&&!"".equals(map.get(obj))&&!"null".equals(map.get(obj))){
                signStr+=obj.toString()+"="+map.get(obj);
            }
        }
        return  signStr;
    }


    public static void main(String[] args) {
        HashMap map = new HashMap();
        map.put("amount","1.0");
        map.put("extFee","0");
        map.put("ip","192.168.1.1");
        map.put("notifyUrl","http://114.55.124.140:8080/com-xiaoma-webapp/printOrder/payASync");
        map.put("orderType","1");
        map.put("outOrderNo","09bb24b96cc84c13a0ff208c2bb1f80a668");
        map.put("partnerId","xmky");
        map.put("paySubject","aaa");
        map.put("payDesc","aaaaaa");
        map.put("payWayId","4000");
        map.put("platformId","11111111111111");
        map.put("printFee","1.0");
        map.put("productId","2016001");
        map.put("quantity","1");
        map.put("reduceFee","0");
        map.put("returnUrl","http://www.sina.com.cn/");
        map.put("showUrl","http://www.baidu.com/");
        map.put("source","2");
        map.put("userId","o2fK_szt9bpsVCK9yKpNeHfCNb8U");
        String param = HttpSendUtil.getParamStrWithoutEncoder(map);
        String sign = DigestUtils.md5Hex(param + "b3e429d568643330f4ee7b0d069283f6");
        map.put("sign",sign);
        String paramStr = HttpSendUtil.getParamStr(map);
        String result = HttpSendUtil.sendPost("http://106.75.87.218/xiaoma-pay-webapp/m/gateway", paramStr);
        System.out.println(result);

//        HttpSendUtil.sendPost("http://114.55.124.140:8080/com-xiaoma-webapp/printOrder/getOrderList","");
    }

}