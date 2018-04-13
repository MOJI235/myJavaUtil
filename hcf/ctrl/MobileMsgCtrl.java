package com.hiyo.hcf.ctrl;

import com.hiyo.hcf.exception.HandleExceptionController;
import com.hiyo.hcf.model.Oem;
import com.hiyo.hcf.service.OemService;
import com.hiyo.hcf.util.SmsUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangshuo on 17/1/16.
 */
@Controller
public class MobileMsgCtrl extends HandleExceptionController {
    @Resource
    private OemService oemService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据类型发送验证码
     *
     * @param request
     * @param response
     * @param mobile   手机号
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/api/sendMsg")
    public void sendMsg(HttpServletRequest request, HttpServletResponse response,
                        @RequestParam(value = "version", required = true) String version,
                        @RequestParam(value = "mobile", required = true) String mobile
    ) {


        Oem oem = oemService.getOemByCode("muyu");
        //生成随机的验证码
        Integer smsCode = (100000 + new Random().nextInt(899999));

        //放进session
        HttpSession session = request.getSession();
        redisTemplate.opsForValue().set(mobile, smsCode + "");
        redisTemplate.expire(mobile, 5, TimeUnit.MINUTES);

        if ("1.0.0".equals(version)) {
            boolean isMobile = mobile.matches("[0-9]{11}");
            JSONObject json = new JSONObject();
            if (isMobile) {
                boolean result =
                        SmsUtil.getInstance(oem).sendSms(mobile, oem.getSmsCodeTemplate(), new String[]{smsCode.toString()});
//                String smsCode1 = request.getSession().getAttribute("smsCode").toString();

                if (result) {
                    json.put("success", true);
                    json.put("message", "成功");
                } else {
                    json.put("success", false);
                    json.put("message", "发送失败");
                }
            } else {
                json.put("success", false);
                json.put("message", "无效的手机号");
            }
            writeResponse(json);
        } else {
            writeException("没有此版本接口");
        }
    }
}
