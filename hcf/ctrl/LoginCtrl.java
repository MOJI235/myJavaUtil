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
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by dishi on 2017/3/23.
 */
@Controller
public class LoginCtrl extends HandleExceptionController {
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
     * 手机号密码登录
     *
     * @param request
     * @param response
     * @param mobile
     * @param password
     * @param version
     */
    @ResponseBody
    @RequestMapping(value = "/api/login", method = {RequestMethod.POST, RequestMethod.GET})
    public void login(HttpServletRequest request, HttpServletResponse response,
                      @RequestParam(value = "mobile", required = true) String mobile,
                      @RequestParam(value = "version", required = true) String version,
                      @RequestParam(value = "password", required = true) String password
    ) {

        if (mobile == null || mobile.length() <= 0) {
            writeException("手机号不能为空");
            return;
        }
        if (password == null || password.length() <= 0) {
            writeException("密码不能为空");
            return;
        }

        if ("1.0.0".equals(version)) {
            String hql = "select a from com.hiyo.hcf.model.Account a where a.mobile = :mobile and a.password = :password";
            HashMap map = new HashMap();
            map.put("mobile", mobile);
            map.put("password", password);
            List<Account> list = accountService.search(hql, map);
            if (list.size() > 0) {
                Account account = list.get(0);
                String token = StringUtil.getUUID();

                redisTemplate.opsForValue().set(token, account.getId()+"");
                redisTemplate.expire(token, 365, TimeUnit.DAYS);
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("accountId", account.getId());
                result.put("token", token);
                writeResponse(result);
            } else {
                writeException("账号或密码不正确");
            }
        } else {
            writeException("没有此版本接口");
        }
    }


    /**
     * 微信app授权登录
     *
     * @param request
     * @param response
     * @param unionId
     * @param version
     * @param source
     */
    @RequestMapping(value = "/api/login/wechatApp")
    public void wechatApp(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam(value = "unionId", required = true) String unionId,
                          @RequestParam(value = "version", required = true) String version,
                          @RequestParam(value = "source", required = true) int source) {
        if ("1.0.0".equals(version)) {
            Account account = accountService.isUnionIdExist(unionId);
            //判断是否注册过
            if (account == null) {
                //1.没注册过保存用户到数据库
                account = new Account();
                account.setUnionId(unionId);
                account.setUserType(Account.userType_wechat_app);
                account.setSource(source);
                JSONObject jsonObject = add(request, response, accountService, account, null);
                //添加账户
                Balance balance = new Balance();
                balance.setAccount(account);
                balance.setBalanceStatus(Balance.balanceStatus_off);
                balanceService.save(balance);
            }
            String token = StringUtil.getUUID();
            redisTemplate.opsForValue().set(token, account.getId()+"");
            redisTemplate.expire(token, 365, TimeUnit.DAYS);
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("accountId", account.getId());
            result.put("token", token);
            writeResponse(result);
        } else {
            writeException("没有此版本接口");
        }
    }

}
