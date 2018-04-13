package com.hiyo.hcf.util;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.cloopen.rest.sdk.CCPRestSDK;
import com.cloopen.rest.sdk.CCPRestSmsSDK;
import com.hiyo.hcf.model.Oem;
import com.hiyo.hcf.service.MobileMsgService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ClassName:SmsUtil
 * Descrpiption: 手机短信工具类
 * User: Wangshuo
 * Date: 2016/9/1 17:30
 */
@Component
public class SmsUtil {

    private static Log logger = LogFactory.getLog(SmsUtil.class);
    private static Map<String, SmsUtil> map = new HashMap<String, SmsUtil>();
    private Oem oem;

    public SmsUtil() {
    }

    public static SmsUtil getInstance(Oem oem) {
        if (!map.containsKey(oem.getSmsAccountSID())) {
            map.put(oem.getSmsAccountSID(),new SmsUtil(oem));
        }
        return map.get(oem.getSmsAccountSID());
    }

    private SmsUtil(Oem oem) {
        this.oem = oem;
    }

    public boolean sendIdentifyingCodeVoice(String phone, String displayNum, String code, String notifyUrl) {
        HashMap<String, Object> result = null;

        CCPRestSDK restAPI = new CCPRestSDK();
        restAPI.init("app.cloopen.com", "8883");// 初始化服务器地址和端口，格式如下，服务器地址不需要写https://
        restAPI.setAccount(oem.getSmsAccountSID(), oem.getSmsAuthToken());// 初始化主帐号和主帐号TOKEN
        restAPI.setAppId(oem.getSmsAppId());// 初始化应用ID
        result = restAPI.voiceVerify(code, phone, displayNum, "3", notifyUrl, "zh", "", "", "");

        logger.error("SDKTestVoiceVerify result=" + result);
        if ("000000".equals(result.get("statusCode"))) {
            //正常返回输出data包体信息（map）
            HashMap<String, Object> data = (HashMap<String, Object>) result.get("data");
            Set<String> keySet = data.keySet();
            for (String key : keySet) {
                Object object = data.get(key);
                logger.error(key + " = " + object);
            }
            logger.warn("PUT THE CODE IN THE SESSION BY YOURSELF !!!");
            return true;
        } else {
            //异常返回输出错误码和错误信息
            logger.error("错误码=" + result.get("statusCode") + " 错误信息= " + result.get("statusMsg"));
        }
        return false;
    }

    /**
     * @return 正常(result.get("statusCode") ="000000")返回输出data包体信息（result.get("data")）,
     *         异常返回输出错误码和错误信息(result.get("statusCode")+result.get("statusMsg"))
     */
    public boolean sendSms(String phone, String templateId, String[] params) {
        HashMap<String, Object> result = null;

        CCPRestSmsSDK restAPI = new CCPRestSmsSDK();
        //restAPI.init("sandboxapp.cloopen.com", "8883");
        restAPI.init("app.cloopen.com", "8883");
//        restAPI.setAccount("8a216da85660607901566a1558110500","d87292d5b78d4527b5fdb46f838abc8a");
//        restAPI.setAppId("8a216da85660607901566a15586c0506");
        restAPI.setAccount(oem.getSmsAccountSID(), oem.getSmsAuthToken());
        restAPI.setAppId(oem.getSmsAppId());
        result = restAPI.sendTemplateSMS(phone, templateId, params);


        logger.info("SDKTestGetSubAccounts: result=" + result + ",sid=,token=,params=" + Arrays.asList(params));
        if ("000000".equals(result.get("statusCode"))) {
            HashMap<String, Object> data = (HashMap<String, Object>) result.get("data");
            Set<String> keySet = data.keySet();
            for (String key : keySet) {
                Object object = data.get(key);
                logger.info(key + " = " + object);
            }
            logger.warn("PUT THE CODE IN THE SESSION BY YOURSELF !!!");
            return true;
        } else {
            logger.warn("错误码=" + result.get("statusCode") + " 错误信息= " + result.get("statusMsg"));
        }

        return false;
    }

    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a","1");
        jsonObject.put("b","2");
        jsonObject.put("c","3");
        jsonObject.put("d","4");
        System.out.println(jsonObject.toString());
    }
}
