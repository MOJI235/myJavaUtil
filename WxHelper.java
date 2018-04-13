package com.hiyo.hcf.util;


import com.hiyo.hcf.model.Oem;
import com.tencent.common.RandomStringGenerator;
import com.tencent.common.Signature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.dswf.Context;
import org.dswf.util.S;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.*;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * User: Administrator
 * Date: 14-8-31
 * Time: 下午2:16
 */
public class WxHelper {

    private Log logger = LogFactory.getLog(WxHelper.class);

    //    private String access_token;
//    private String lastTokenTime;
//    private String jsapi_ticket;
//    private String lastTokenTime_for_jsapi_ticket;
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    private final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    //private static Map<String, WxHelper> map = new HashMap<String, WxHelper>();
    private RedisTemplate redisTemplate;

    public WxHelper(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static class TemplateMessageData extends JSONObject {

        public TemplateMessageData add(String name, String value) {
            return add(name, value, "#000000");
        }

        public TemplateMessageData add(String name, String value, String color) {
            JSONObject a = new JSONObject();
            a.put("value", value);
            a.put("color", color);
            put(name, a);
            return this;
        }

        public JSONObject getJsonObject() {
            return this;
        }

        public static TemplateMessageData create() {
            return new TemplateMessageData();
        }
    }

    private boolean isTokenError(JSONObject jsonObject) {
        if (!jsonObject.has("errcode")) {
            return false;
        }
        String errorCode = jsonObject.getString("errcode");
        return ("40001".equals(errorCode) || "40014".equals(errorCode) || "42001".equals(errorCode));
    }


    /**
     * 网页授权后拉取用户信息，使用授权后给与的Access_Token
     *
     * @param openid
     * @return
     */
    public JSONObject getWxUserInfoByOauth(String openid, String oauth2_access_token) {
        String url = "https://api.weixin.qq.com/sns/userinfo?" +
                "access_token=" + oauth2_access_token + "&openid=" + openid + "&lang=zh_CN";
        JSONObject jsonObject = new JSONObject(HttpUtil.httpGet(url));
        logger.info(url + ":" + jsonObject);
        return jsonObject;

    }

    public JSONObject getWxUserInfoByOpenId(Oem oem, String openid) throws Exception {
        String url = "https://api.weixin.qq.com/cgi-bin/user/info?" +
                "access_token=" + getToken(oem) + "&openid=" + openid + "&lang=zh_CN";

        JSONObject jsonObject = new JSONObject(HttpUtil.httpGet(url));
        logger.info(url + ":" + jsonObject);
        if (isTokenError(jsonObject)) {
            resetToken(oem);
            return getWxUserInfoByOpenId(oem, openid);
        } else {
            return jsonObject;
        }
    }


    public String[] getOpenIdByCode(Oem oem, String code) {
        try {
            String url = "https://api.weixin.qq.com/sns/oauth2/access_token?" +
                    "appid=" + oem.getWxAppId() +
                    "&secret=" + oem.getWxAppSecret() +
                    "&code=" + code +
                    "&grant_type=authorization_code";
            JSONObject jsonObject = new JSONObject(HttpUtil.httpGet(url));
            logger.info(url + ":" + jsonObject);
            String access_token = jsonObject.getString("access_token");
            String unionid = jsonObject.getString("unionid");
            return new String[]{jsonObject.getString("openid"), access_token, unionid};
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }


//    public String getAuthUrl(Oem oem, String url) throws Exception {
//        url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + oem.getWxAppId() + "&redirect_uri=" + URLEncoder.encode(url, "utf-8") + "&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
//        logger.info(url);
//        return url;
//    }




    public Map<String, String> sign(Oem oem, String url) {
        try {
            Map<String, String> ret = new HashMap<String, String>();
            String nonce_str = create_nonce_str();
            String timestamp = create_timestamp();
            String string1;
            String signature = "";
            //注意这里参数名必须全部小写，且必须有序
            string1 = "jsapi_ticket=" + getJsApiTicket(oem) +
                    "&noncestr=" + nonce_str +
                    "&timestamp=" + timestamp +
                    "&url=" + url;
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(string1.getBytes("UTF-8"));
            signature = byteToHex(crypt.digest());
            ret.put("url", url);
            ret.put("jsapi_ticket", getJsApiTicket(oem));
            ret.put("nonceStr", nonce_str);
            ret.put("timestamp", timestamp);
            ret.put("signature", signature);
            return ret;
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }


    public synchronized String getToken(Oem oem) throws Exception {
        if (redisTemplate.hasKey(oem.getWxAppId())) {
            return redisTemplate.opsForValue().get(oem.getWxAppId()).toString();
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + oem.getWxAppId() + "&secret=" + oem.getWxAppSecret();
        JSONObject jsonObject = new JSONObject(HttpUtil.httpGet(url));
        logger.info(url + ":" + jsonObject);
        String access_token = jsonObject.getString("access_token");
        String key = oem.getWxAppId() + ".access_token";
        redisTemplate.opsForValue().set(key, access_token);
        redisTemplate.expire(key, 6000, TimeUnit.SECONDS);
        return access_token;
    }

    private void resetToken(Oem oem) {
        if (redisTemplate.hasKey(oem.getWxAppId() + ".access_token")) {
            redisTemplate.delete(oem.getWxAppId() + ".access_token");
        }
    }

    private String getJsApiTicket(Oem oem) throws Exception {

        String key = oem.getWxAppId() + ".jsapi_ticket";

        if (redisTemplate.hasKey(key)) {
            return redisTemplate.opsForValue().get(key).toString();
        }
        String access_token = getToken(oem);
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=" + access_token + "&type=jsapi";
        JSONObject jsonObject = new JSONObject(HttpUtil.httpGet(url));
        logger.info(url + ": " + jsonObject);
        if (isTokenError(jsonObject)) {
            resetToken(oem);
            return getJsApiTicket(oem);
        } else {
            if (jsonObject.has("ticket")) {
                String jsapi_ticket = jsonObject.getString("ticket");
                redisTemplate.opsForValue().set(key, jsapi_ticket);
                redisTemplate.expire(key, 6000, TimeUnit.SECONDS);
                return jsapi_ticket;
            }
        }
        return null;
    }

    public String shortUrl(Oem oem, String longUrl) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("action", "long2short");
            jsonObject.put("long_url", longUrl);
            jsonObject.put("access_token", getToken(oem));
            String url = "https://api.weixin.qq.com/cgi-bin/shorturl?access_token=" + getToken(oem);
            String str = HttpUtil.httpPost(url, null, new StringEntity(jsonObject.toString()));
            logger.info(url + ":" + str);
            jsonObject = new JSONObject(str);
            if (isTokenError(jsonObject)) {
                resetToken(oem);
                return shortUrl(oem, longUrl);
            } else {
                if (!jsonObject.isNull("short_url")) {
                    return jsonObject.getString("short_url");
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }


    private String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private String create_nonce_str() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    private String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

    public String sendTemplateMessage(
            final Oem oem,
            final String openId,
            final String templateId,
            final String url,
            final JSONObject data) {
        if (S.isEmpty(templateId)) {
            logger.warn("template message not send: " + openId);
            return "success";
        }
        executor.submit(
                new Runnable() {
                    public void run() {
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("touser", openId);
                            jsonObject.put("template_id", templateId);
                            jsonObject.put("url", url);
                            jsonObject.put("data", data);
                            getToken(oem);
                            String _url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + getToken(oem);
                            String str = HttpUtil.httpPost(_url, null, new StringEntity(jsonObject.toString(), "utf-8"));
                            JSONObject respJson = new JSONObject(str);
                            logger.info(_url + ":" + respJson);
                            if (isTokenError(respJson)) {
                                resetToken(oem);
                                run();
                            }
                        } catch (Exception e) {
                            logger.error("", e);
                        }

                    }
                }
        );

        return "success";
    }

    public void sendServiceMessage(Oem oem, String openId, String content) throws Exception {
        String url = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";
        net.sf.json.JSONObject contentObj = new net.sf.json.JSONObject();
        contentObj.put("content", content);
        net.sf.json.JSONObject messageObj = new net.sf.json.JSONObject();
        messageObj.put("touser", openId);
        messageObj.put("msgtype", "text");
        messageObj.put("text", contentObj);
        String str = HttpUtil.httpPost(url + getToken(oem), null, new StringEntity(messageObj.toString(), "utf-8"));
        logger.info(url + ":" + str);
    }


    /**
     * 转发消息到多客服系统
     *
     * @param toUserName
     * @param fromUserName
     * @throws Exception
     */
    public Element sendToMultiCustomerServiceSystem(String toUserName, String fromUserName) throws Exception {
        Element resEle = new Element("xml");
        resEle.addContent(new Element("ToUserName").addContent(new CDATA(toUserName)));
        resEle.addContent(new Element("FromUserName").addContent(new CDATA(fromUserName)));
        resEle.addContent(new Element("CreateTime").setText(Long.toString(System.currentTimeMillis())));
        resEle.addContent(new Element("MsgType").addContent(new CDATA("transfer_customer_service")));
        //resEle.addContent(new Element("TransInfo")).addContent(new Element("KfAccount").setText("tanzixi@hiyou-hiyou")); //书记

        return resEle;
    }

    public Element sendText(String toUserName, String fromUserName, String responseText) throws Exception {
        Element resEle = new Element("xml");
        resEle.addContent(new Element("ToUserName").addContent(new CDATA(toUserName)));
        resEle.addContent(new Element("FromUserName").addContent(new CDATA(fromUserName)));
        resEle.addContent(new Element("CreateTime").setText(Long.toString(System.currentTimeMillis())));
        resEle.addContent(new Element("MsgType").addContent(new CDATA("text")));
        resEle.addContent(new Element("Content").addContent(new CDATA(responseText)));
        return resEle;
    }

    public Element sendTextAndImg(String toUserName, String fromUserName, String title, String description, String picUrl, String url) throws Exception {
        Element resEle = new Element("xml");
        resEle.addContent(new Element("ToUserName").addContent(new CDATA(toUserName)));
        resEle.addContent(new Element("FromUserName").addContent(new CDATA(fromUserName)));
        resEle.addContent(new Element("CreateTime").setText(Long.toString(System.currentTimeMillis())));
        resEle.addContent(new Element("MsgType").addContent(new CDATA("news")));
        resEle.addContent(new Element("ArticleCount").addContent("1"));
        Element articles = new Element("Articles");
        resEle.addContent(articles);

        Element item = new Element("item");
        item.addContent(new Element("Title").addContent(new CDATA(title)));
        item.addContent(new Element("Description").addContent(new CDATA(description)));
        item.addContent(new Element("PicUrl").addContent(new CDATA(picUrl)));
        item.addContent(new Element("Url").addContent(new CDATA(url)));
        articles.addContent(item);
        return resEle;
    }

    public String encode(String algorithm, String str) {
        if (str == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(str.getBytes());
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (int j = 0; j < len; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    public JSONObject getGroup(Oem oem) throws Exception {
        String s = HttpUtil.httpGet("https://api.weixin.qq.com/cgi-bin/groups/get?access_token=" + getToken(oem));
        return new JSONObject(s);
    }

    public JSONObject changeGroup(Oem oem, int group, String openId) throws Exception {
        getToken(oem);
        String c = "{\"openid\":\"" + openId + "\",\"to_groupid\":" + group + "}";
        String s = HttpUtil.httpPost("https://api.weixin.qq.com/cgi-bin/groups/members/update?access_token=" + getToken(oem), null, new StringEntity(c));
        return new JSONObject(s);
    }

    private String genQRCode(Oem oem, String scene_id, boolean infinite) throws Exception {
        String access_token = getToken(oem);
        String url = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=" + access_token;
        String str;
        String reqStr;
        if (!infinite) {
            reqStr = "{\"expire_seconds\": 2592000, \"action_name\": \"QR_SCENE\", \"action_info\": {\"scene\": {\"scene_id\": " + scene_id + "}}}";
            str = org.dswf.util.HttpUtil.httpPost(url, null,
                    new StringEntity(reqStr));
        } else {
            if (scene_id.matches("[0-9]+")) {
                reqStr = "{\"action_name\": \"QR_LIMIT_SCENE\", \"action_info\": {\"scene\": {\"scene_id\": " + scene_id + "}}}";
            } else {
                reqStr = "{\"action_name\": \"QR_LIMIT_STR_SCENE\", \"action_info\": {\"scene\": {\"scene_str\": \"" + scene_id + "\"}}}";
            }
            str = org.dswf.util.HttpUtil.httpPost(url, null,
                    new StringEntity(reqStr));
        }
        logger.info("genQRCode: reqStr=" + reqStr + ", reqStr=" + str);
        JSONObject jsonObject = new JSONObject(str);
        if (S.isNotEmpty(jsonObject.getString("errcode"))
                &&
                ("40001".equals(jsonObject.getString("errcode"))
                        ||
                        "40014".equals(jsonObject.getString("errcode"))
                        ||
                        "42001".equals(jsonObject.getString("errcode")))
                ) {
            resetToken(oem);
        } else if (!jsonObject.isNull("ticket")) {
            String ticket = jsonObject.getString("ticket");
            return jsonObject.getString("url");
        }
        return null;
    }

    public String getQrCode(Oem oem, String qrscene, boolean infinite) {
        for (int i = 0; i < 5; i++) {
            try {
                File f = new File(Context.getRequest().getRealPath("/") + "/upload/tmp/" + qrscene + ".json");
                String s = null;
                if (f.exists()
                        && f.length() > 0) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
                    while (true) {
                        String l = br.readLine();
                        if (l == null) {
                            break;
                        }
                        sb.append(l);
                    }
                    br.close();
                    JSONObject object = new JSONObject(sb.toString());
                    long time = object.getLong("time");
                    if (System.currentTimeMillis() - time < 9 * 24 * 3600 * 1000l
                            || infinite) {
                        s = object.getString("url");
                    }
                }
                if (S.isEmpty(s)) {
                    s = genQRCode(oem, qrscene, infinite);
                    if (S.isNotEmpty(s)) {
                        logger.info("getQrCode: s=" + s);
                        JSONObject object = new JSONObject();
                        object.put("time", System.currentTimeMillis());
                        object.put("infinite", S.isNotEmpty(infinite) ? infinite : 0);
                        object.put("url", s);
                        if (!f.getParentFile().exists()) {
                            f.getParentFile().mkdirs();
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(object.toString().getBytes());
                            fos.close();
                        } catch (IOException e) {

                        }
                    } else {
                        logger.info("getQrCode failed: s=" + s);
                    }
                } else {
                    logger.info("getQrCode from file: s=" + s);
                }
                return s;
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        return null;
    }

    public void createMenu(Oem oem) throws Exception {
        HttpClient httpClient = HttpUtil.getHttpClient();
        String access_token = getToken(oem);
        String url = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + access_token;
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("./menu.json")));
        while (true) {
            String s = br.readLine();
            if (s == null) {
                break;
            }
            sb.append(s);
        }
        br.close();
        String s = sb.toString().replace("OEMCODE", oem.getCode());
        s = s.replace("HOST", oem.getHost());
        System.out.println(s);
        HttpPost createPost = new HttpPost(url);
        createPost.setEntity(new StringEntity(s, "utf-8"));
        HttpResponse createResp = httpClient.execute(createPost);
        if (HttpStatus.SC_OK == createResp.getStatusLine().getStatusCode()) {
            String str = EntityUtils.toString(createResp.getEntity(), "utf-8");
            System.out.println(str);
        }
    }

    public String toTransfer(Oem oem,
                             String bizId,
                             String mch_id,
                             String wx_key) throws Exception {
        String nonce_str = RandomStringGenerator.getRandomStringByLength(32);
        getToken(oem);
        Element xml = new Element("xml");
        HashMap map = new HashMap();
        map.put("partner_trade_no", bizId);
        map.put("mch_id", mch_id);
        map.put("appid", oem.getWxAppId());
        map.put("nonce_str", nonce_str);

        xml.addContent(new Element("partner_trade_no").addContent(bizId));
        xml.addContent(new Element("mch_id").addContent(mch_id));
        xml.addContent(new Element("appid").addContent(oem.getWxAppId()));
        xml.addContent(new Element("nonce_str").addContent(nonce_str));
        xml.addContent(new Element("sign").addContent(Signature.getSign(wx_key, map)));

        StringWriter sw = new StringWriter();
        new XMLOutputter(Format.getPrettyFormat()).output(xml, sw);
        logger.info(sw.toString());
        String s = org.dswf.util.HttpUtil.httpPost(oem.getCode(), "https://api.mch.weixin.qq.com/mmpaymkttransfers/gettransferinfo", null, new StringEntity(sw.toString(), "utf-8"));
        Element respXml = new SAXBuilder().build(new StringReader(s)).getRootElement();
        if ("success".equalsIgnoreCase(respXml.getChildText("result_code"))) {
            return null;
        } else {
            return s;
        }
    }

    public String transfer(Oem oem,
                           String bizId,
                           String mch_id,
                           String wx_key,
                           String spbill_create_ip,
                           String openId,
                           String realName,
                           int amount) throws Exception {
        String nonce_str = RandomStringGenerator.getRandomStringByLength(32);
        getToken(oem);
        Element xml = new Element("xml");
        HashMap map = new HashMap();
        map.put("mch_appid", oem.getWxAppId());
        map.put("mchid", mch_id);
        map.put("nonce_str", nonce_str);
        map.put("partner_trade_no", bizId);
        map.put("openid", openId);
        map.put("check_name", "NO_CHECK");
//        map.put("re_user_name", realName);
        map.put("amount", amount);
        map.put("desc", bizId);
        map.put("spbill_create_ip", spbill_create_ip);

        xml.addContent(new Element("mch_appid").addContent(oem.getWxAppId()));
        xml.addContent(new Element("mchid").addContent(mch_id));
        xml.addContent(new Element("nonce_str").addContent(nonce_str));
        xml.addContent(new Element("partner_trade_no").addContent(bizId));
        xml.addContent(new Element("openid").addContent(openId));
        xml.addContent(new Element("check_name").addContent("NO_CHECK"));
//        xml.addContent(new Element("re_user_name").addContent(realName));
        xml.addContent(new Element("amount").addContent(amount + ""));
        xml.addContent(new Element("desc").addContent(bizId));
        xml.addContent(new Element("spbill_create_ip").addContent(spbill_create_ip));
        xml.addContent(new Element("sign").addContent(Signature.getSign(wx_key, map)));


        StringWriter sw = new StringWriter();
        new XMLOutputter(Format.getPrettyFormat()).output(xml, sw);
        logger.info(sw.toString());
        String s = org.dswf.util.HttpUtil.httpPost(oem.getCode(), "https://api.mch.weixin.qq.com/mmpaymkttransfers/promotion/transfers", null, new StringEntity(sw.toString(), "utf-8"));
        Element respXml = new SAXBuilder().build(new StringReader(s)).getRootElement();
        logger.info(s);
        if ("success".equalsIgnoreCase(respXml.getChildText("result_code"))) {
            /**
             * <xml>
             <return_code><![CDATA[SUCCESS]]></return_code>
             <return_msg><![CDATA[参数错误:输入的商户号有误.]]></return_msg>
             <result_code><![CDATA[FAIL]]></result_code>
             <err_code><![CDATA[PARAM_ERROR]]></err_code>
             <err_code_des><![CDATA[参数错误:输入的商户号有误.]]></err_code_des>
             </xml>
             */
            return null;
        } else {
            return respXml.getChildText("result_code") + ", " + respXml.getChildText("return_msg");
        }
    }
}
