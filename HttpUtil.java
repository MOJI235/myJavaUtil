package com.hiyo.hcf.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUtil {

    //private static DefaultHttpClient httpClient;

    private static Map<String, DefaultHttpClient> httpClientMap = new HashMap<String, DefaultHttpClient>();

    private static Log logger = LogFactory.getLog(HttpUtil.class);

    private HttpUtil() {
    }

    public static synchronized HttpClient getHttpClient() {

        return getHttpClient("default");
    }

    public static List<BasicNameValuePair> map2list(Map<String,String> map) {
        Iterator<String> iterator = map.keySet().iterator();
        List<BasicNameValuePair> l = new ArrayList<BasicNameValuePair>();
        while (iterator.hasNext()) {
            String name = iterator.next();
            String value = map.get(name);
            l.add(new BasicNameValuePair(name,value));
        }
        return l;
    }

    public static synchronized HttpClient getHttpClient(String id) {

        if (null == httpClientMap.get(id)) {
            // 初始化工作
            try {
                httpClientMap.put(id, createHttpClient());
            } catch (Exception e) {
                e.printStackTrace();
                return new DefaultHttpClient();
            }
        }
        return httpClientMap.get(id);
    }

    private static DefaultHttpClient createHttpClient() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);  //允许所有主机的验证

        BasicHttpParams params = new BasicHttpParams();

        HttpProtocolParams.setContentCharset(params, "utf-8");
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);

        // 设置连接管理器的超时
        ConnManagerParams.setTimeout(params, 30000);
        // 设置连接超时
        HttpConnectionParams.setConnectionTimeout(params, 30000);
        // 设置socket超时
        HttpConnectionParams.setSoTimeout(params, 30000);



        // 设置http https支持
        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));
        schReg.register(new Scheme("https", sf, 443));

        ClientConnectionManager conManager = new ThreadSafeClientConnManager(
                params, schReg);

        DefaultHttpClient httpClient = new DefaultHttpClient(conManager, params);



        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount >= 3) {
                    // 如果超过最大重试次数，那么就不要继续了
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // 如果服务器丢掉了连接，那么就重试
                    return true;
                }
                if (exception instanceof SSLHandshakeException) {
                    // 不要重试SSL握手异常
                    return false;
                }
                HttpRequest request = (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // 如果请求被认为是幂等的，那么就重试
                    return true;
                }
                return false;
            }
        };
        httpClient.setHttpRequestRetryHandler(httpRequestRetryHandler);
        return httpClient;
    }

    public static class RequestHeader extends LinkedHashMap {

        public RequestHeader add(String name, String value) {
            put(name, value);
            return this;
        }
    }

    public static RequestHeader createRequestHeader() {
        return new RequestHeader();
    }

    public static String httpGet(String url) {
        return httpGet(url, new LinkedHashMap());
    }

    private static String getEncoding(Header contentType, byte[] bytes) throws Exception {
        Pattern p1 = Pattern.compile("content=\"(.*?);\\s?charset=(.*?)\"");
        Pattern p2 = Pattern.compile("<meta\\s+charset=\"(.*?)\">");
        Pattern p3 = Pattern.compile("(.*?);\\s?(charset|encoding)=(.*)");//
        String encoding = "utf-8";
        // <meta charset="gb2312">
        // content="text/html; charset=utf-8"
        if (contentType != null
                && contentType.getValue().contains("charset")) {
            Matcher m3 = p3.matcher(contentType.getValue());
            if (m3.find()) {
                encoding = m3.group(3);
                return encoding;
            }
        }
        String str = new String(bytes, "utf-8");
        Matcher m1 = p1.matcher(str);
        if (m1.find()) {
            encoding = m1.group(2);
            return encoding;
        } else {
            Matcher m2 = p2.matcher(str);
            if (m2.find()) {
                encoding = m2.group(1);
                return encoding;
            }
        }
        return encoding;
    }

    public static String httpGet(String strUrl, LinkedHashMap headers) {
        HttpGet httpGet = null;
        try {
            URL url = new URL(strUrl);
            URI uri = new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null);

            httpGet = new HttpGet(uri);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
            httpGet.setConfig(requestConfig);

            if (headers != null) {
                Iterator iterator = headers.keySet().iterator();
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    httpGet.addHeader(next.toString(), headers.get(next).toString());
                }
            }

            HttpResponse resp = getHttpClient().execute(httpGet);
            Header contentType = resp.getFirstHeader("Content-Type");
            byte[] bytes = EntityUtils.toByteArray(resp.getEntity());
            String encoding = getEncoding(contentType, bytes);
            return new String(bytes, encoding);
        } catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException(e);
        } finally {
            try {
                if (httpGet!=null) {
                    httpGet.abort();
                }
            } catch (Exception e) {

            }
        }
    }

    public static String httpPost(String url, LinkedHashMap headers, HttpEntity entity) {
        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
        httpPost.setConfig(requestConfig);
        try {
            if (headers != null) {
                Iterator iterator = headers.keySet().iterator();
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    httpPost.addHeader(next.toString(), headers.get(next).toString());
                }
            }

            httpPost.setEntity(entity);
            HttpResponse resp = getHttpClient().execute(httpPost);
            Header contentType = resp.getFirstHeader("Content-Type");
            byte[] bytes = EntityUtils.toByteArray(resp.getEntity());
            String encoding = getEncoding(contentType, bytes);
            return new String(bytes, encoding);
        } catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException(e);
        } finally {
            try {
                httpPost.abort();
            } catch (Exception e) {

            }
        }
    }

    public static byte[] httpGetBytes(String url) {
        HttpGet httpGet = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
        httpGet.setConfig(requestConfig);
        try {
            HttpResponse resp = getHttpClient().execute(httpGet);
            return EntityUtils.toByteArray(resp.getEntity());
        } catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException(e);
        } finally {
            try {
                httpGet.abort();
            } catch (Exception e) {

            }
        }
    }

    public static byte[] httpGetBytes(String url, LinkedHashMap headers) {
        HttpGet httpGet = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
        httpGet.setConfig(requestConfig);
        try {
            if (headers != null) {
                Iterator iterator = headers.keySet().iterator();
                while (iterator.hasNext()) {
                    Object next = iterator.next();
                    httpGet.addHeader(next.toString(), headers.get(next).toString());
                }
            }
            HttpResponse resp = getHttpClient().execute(httpGet);
            return EntityUtils.toByteArray(resp.getEntity());
        } catch (Exception e) {
            logger.error("", e);
            throw new RuntimeException(e);
        } finally {
            try {
                httpGet.abort();
            } catch (Exception e) {

            }
        }
    }

    public static void main(String[] args) {
        String s = HttpUtil.httpGet("http://zzd.sm.cn/webapp/webview/article/ucpush?aid=17078522291549012972&cid=1192652582&uc_param_str=dnnivebichfrmintcpgieiwidsudpf&btifl=100&zzd_from=ucpush&dn=7523560642-7876be61&fr=android&pf=145&bi=800&ve=1.8.0.0&ss=392x698&cp=isp:%E7%A7%BB%E5%8A%A8;prov:%E4%B8%8A%E6%B5%B7;city:%E4%B8%8A%E6%B5%B7;na:%E4%B8%AD%E5%9B%BD;cc:CN;ac:&gi=bTkwBBnJ81ezUiuGOGVWuGsCt3XkHQij6QOY54K8UyJ6bxs%3D&mi=x600&bt=UM&bm=P3W&nt=1&ni=bTkwBPH8PliGU5uNzLCaszquLXi548%2FiYuW7tQgfLSte1Q%3D%3D&si=bTkwBM8b%2Bgqhs1ZfjZRbAfTtpZlzIk306A%3D%3D&ei=bTkwBO8Xfl6Nt9z36JvftTborWS5O08%2F6w%3D%3D&jb=0&la=zh-CN&og=GR&gd=bTkwBCkOBWmuHymYdetB3k784ieMkOe6a2OHTKEw8AXFLEjhhfTH8DqmO2zkRPNN0KPjosmz&uc_biz_str=S:custom|C:iflow_hide|K:true&tt_from=uc_btn&pagetype=share&rd_type=share&refrd_id=bf8bc66280da676484781a61bf5728a0&app=uc-iflow&sn=8439586385465044834");
        System.out.println(s);
    }

}

class SSLSocketFactoryEx extends SSLSocketFactory {

    SSLContext sslContext = SSLContext.getInstance("TLS");

    public SSLSocketFactoryEx(KeyStore truststore)
            throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, UnrecoverableKeyException {
        super(truststore);

        TrustManager tm = new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] chain, String authType)
                    throws CertificateException {

            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] chain, String authType)
                    throws CertificateException {

            }
        };

        sslContext.init(null, new TrustManager[]{tm}, null);
    }


    public Socket createSocket(Socket socket, String host, int port,
                               boolean autoClose) throws IOException, UnknownHostException {
        return sslContext.getSocketFactory().createSocket(socket, host, port,
                autoClose);
    }


    public Socket createSocket() throws IOException {
        return sslContext.getSocketFactory().createSocket();
    }

    public static void main(String[] args) throws Exception {
        String s = HttpUtil.httpGet("https://truthcontentzc.tmall.com/");
        System.out.println(s);
    }
}
