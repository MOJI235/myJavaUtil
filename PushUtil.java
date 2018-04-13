package com.hiyo.hcf.util;

import cn.jpush.api.JPushClient;
import cn.jpush.api.common.ClientConfig;
import cn.jpush.api.common.resp.APIConnectionException;
import cn.jpush.api.common.resp.APIRequestException;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import com.hiyo.hcf.model.Oem;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * ClassName:PushUtil
 * Descrpiption:推送相关
 * User: Wangshuo
 * Date: 2016/10/6 16:06
 */
public class PushUtil {

    //    private static final String masterSecret = "069f36369ffabaeae99f573c";
//    private static final String appKey = "06d16421489214d7805cc060";

//    private static final String appKey = Config.getConfig().get("jpush_app_key");
//    private static final String masterSecret = Config.getConfig().get("jpush_master_secret");

    private static Logger logger = Logger.getLogger(PushUtil.class);
    private Oem oem;
    private static Map<String, PushUtil> map = new HashMap<String, PushUtil>();

    public PushUtil(Oem oem) {
        this.oem = oem;
    }

    public static PushUtil getInstance(Oem oem) {
        if (!map.containsKey(oem.getJpushAppKey())) {
            map.put(oem.getJpushAppKey(),new PushUtil(oem));
        }
        return map.get(oem.getJpushAppKey());
    }

    public void push(PushPayload payload){

//        JPushClient jpush = new JPushClient(masterSecret, appKey);
        ClientConfig config = ClientConfig.getInstance();
        config.setTimeToLive(86400);
        config.setApnsProduction(true);
        //设置为开发模式
//        config.setApnsProduction(false);
        JPushClient jpush = new JPushClient(oem.getJpushMasterSecret(),oem.getJpushAppKey(),null,config);

        try {
            PushResult result = jpush.sendPush(payload);
        } catch (APIConnectionException e) {
            logger.error("Connection error. Should retry later. ", e);
        } catch (APIRequestException e) {
            logger.error("推送失败:"+e.getErrorMessage());
            logger.info("Msg ID: " + e.getMsgId());
        }
    }

    /**
     * 全平台推送
     * @param alert
     * @return
     */
    private static PushPayload buildPushObject_all_all_alert(String alert){
        return PushPayload.alertAll(alert);
    }

    /**
     * 根据别名推送
     * @param alias
     * @return
     */
    public PushPayload buildAllAliasAlert(String alert, String alias,HashMap map) {

        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            map.put(next,map.get(next).toString());
        }
        return PushPayload.newBuilder()
                .setPlatform(Platform.android_ios())
                .setAudience(Audience.alias(alias))
                .setNotification(Notification.newBuilder()
                        .setAlert(alert)
                        .addPlatformNotification(AndroidNotification.newBuilder()
                                .addExtras(map).build())
                        .addPlatformNotification(IosNotification.newBuilder()
                                .setAlert(alert).addExtras(map)
                               .build())
                        .build())
                .build();
    }
}
