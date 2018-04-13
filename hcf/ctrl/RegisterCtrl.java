package com.hiyo.hcf.ctrl;

import com.hiyo.hcf.exception.HandleExceptionController;
import com.hiyo.hcf.model.Account;
import com.hiyo.hcf.model.Balance;
import com.hiyo.hcf.service.AccountService;
import com.hiyo.hcf.service.BalanceService;
import com.hiyo.hcf.service.ChargeRecordService;
import com.hiyo.hcf.service.MobileMsgService;
import com.hiyo.hcf.util.StringUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * Created by dishi on 2017/3/23.
 */
@Controller
public class RegisterCtrl extends HandleExceptionController {
    @Resource
    private AccountService accountService;
    @Resource
    private MobileMsgService mobileMsgService;
    @Resource
    private BalanceService balanceService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Resource
    private ChargeRecordService chargeRecordService;

    /**
     * 注册
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/api/register", method = {RequestMethod.POST, RequestMethod.GET})
    public void register(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(value = "version", required = true) String version) {
        String mobile = getRequestJsonObj().getString("mobile");
        String password = getRequestJsonObj().getString("password");
        String confirmPassword = getRequestJsonObj().getString("confirmPassword");
        String smsCode = getRequestJsonObj().getString("smsCode");
        Integer source = Integer.parseInt(getRequestJsonObj().getString("source"));
        if ("1.0.0".equals(version)) {
            //1.判断验证码是否正确
            boolean codeResult = mobileMsgService.checkVerifyCode(mobile, smsCode);
            //2.保存用户到数据库
            if (codeResult) {
                //判断用户是否存在
                if (!accountService.isMobileExist(mobile)) {
                    //判端两次输入的密码是否一致
                    if (password.equals(confirmPassword)) {
                        Account account = new Account();
                        account.setMobile(mobile);
                        account.setPassword(password);
                        account.setUserType(Account.userType_mobile);
                        account.setSource(source);
                        JSONObject jsonObject = add(request, response, accountService, account, null);
                        //添加账户
                        Balance balance = new Balance();
                        balance.setAccount(account);
                        balance.setBalanceStatus(Balance.balanceStatus_off);
                        balanceService.save(balance);
                        //生成token
                        String token = StringUtil.getUUID();
                        redisTemplate.opsForValue().set(token, account.getId()+"");
                        redisTemplate.expire(token, 365, TimeUnit.DAYS);
                        JSONObject result = new JSONObject();

                        result.put("success", true);
                        result.put("accountId", account.getId());
                        result.put("token", token);
                        writeResponse(result);
                    } else {
                        writeException("两次输入的密码不一致,请重新输入");
                    }
                } else {
                    writeException("用户已存在");
                }
            } else {
                writeException("验证码错误");
            }
        } else {
            writeException("没有此版本接口");
        }
    }
}
