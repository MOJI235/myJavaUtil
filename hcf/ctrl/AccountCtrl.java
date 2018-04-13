package com.hiyo.hcf.ctrl;

import com.hiyo.hcf.exception.HandleExceptionController;
import com.hiyo.hcf.model.Account;
import com.hiyo.hcf.service.AccountService;
import com.hiyo.hcf.service.BalanceService;
import com.hiyo.hcf.service.ChargeRecordService;
import com.hiyo.hcf.service.MobileMsgService;
import com.hiyo.hcf.util.TokenUtil;
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
import java.text.ParseException;
import java.util.List;

/**
 * 悟
 * <p>
 * 悟道休言天命，修行勿取真经，一悲一喜一枯荣，哪个前生注定？
 * 袈纱本无清静，红尘不染性空，幽幽古刹千年钟，都是痴人说梦。
 * <p>
 * 作者: 邸石
 * 日期: 17/2/8
 * 时间: 下午10:20
 */
@Controller
public class AccountCtrl extends HandleExceptionController {

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
     * 修改用户信息
     *
     * @param request
     * @param response
     * @param token
     * @param version
     */
    @RequestMapping(value = "/api/account/modifyInfo", method = {RequestMethod.POST, RequestMethod.GET})
    public void modifyInfo(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = "token", required = true) String token,
                           @RequestParam(value = "version", required = true) String version) throws ParseException {
        if ("1.0.0".equals(version)) {
            Account account = (Account) parseRequest(Account.class);
            account = accountService.merge(account);
            accountService.update(account);
        } else {
            writeException("没有此版本接口");
        }
    }


    @RequestMapping(value = "/api/account/detail", method = RequestMethod.POST)
    public void detail2(HttpServletRequest request,
                        HttpServletResponse response,
                        @RequestParam(value = "token", required = true) String token,
                        @RequestParam(value = "version", required = true) String version) {

    }


    @RequestMapping(value = "/api/account/forgetPwd", method = RequestMethod.POST)
    public void forgetPwd(HttpServletRequest request,
                          HttpServletResponse response,
                          @RequestParam(value = "token", required = true) String token,
                          @RequestParam(value = "version", required = true) String version) {
        String mobile = getRequestJsonObj().getString("mobile");
        if (mobile == null || mobile.length() <= 0) {
            writeException("手机号不能为空");
            return;
        }
        String hql = "select a from com.hiyo.hcf.model.Account a where " +
                " a.mobile = '" + mobile + "'";
        List l = accountService.search(hql);
        if (l.size() > 0) {
            Account account = (Account) l.get(0);
            account.setPassword(getRequestJsonObj().getString("password"));
            accountService.update(account);
            writeSuccess();
        } else {
            writeException("手机号不存在！");
            return;
        }
        writeSuccess();
    }

    //修改号码
    @RequestMapping(value = "/api/account/resetMobile")
    public void resetMobile(HttpServletRequest request,
                            HttpServletResponse response,
                            @RequestParam(value = "token", required = true) String token,
                            @RequestParam(value = "mobile", required = true) String mobile,
                            @RequestParam(value = "smsCode", required = true) String smsCode,
                            @RequestParam(value = "version", required = true) String version) {
        Long accountId = TokenUtil.getIdByToken(token);
        if ("1.0.0".equals(version)) {
            if (mobile == null || mobile.length() <= 0) {
                writeException("手机号不能为空");
                return;
            }
            if (smsCode == null || smsCode.length() <= 0) {
                writeException("验证码不能为空");
                return;
            }

            if (!redisTemplate.hasKey(mobile)) {
                writeException("验证码失效");
                return;
            } else {
                String smsCode0 = redisTemplate.opsForValue().get(mobile).toString();
                boolean codeResult = smsCode0.equals(smsCode);
                if (!codeResult) {
                    writeException("验证码输入错误");
                }
            }

            Account account = accountService.searchByKey(accountId);
            account.setMobile(mobile);
            accountService.update(account);
            writeSuccess();
        } else {
            writeException("没有此版本接口");
        }
    }

    @RequestMapping(value = "/api/account/modifyPwd")
    public void modifyPwd(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam(value = "token", required = true) String token,
                          @RequestParam(value = "password", required = true) String password,
                          @RequestParam(value = "version", required = true) String version,
                          @RequestParam(value = "newPassword", required = true) String newPassword
    ) {
        Long accountId = TokenUtil.getIdByToken(token);
        if ("1.0.0".equals(version)) {
            if (accountId == -1) {
                writeException("没有登录,请进入登录页面进行登录");
                return;
            }
            if (password == null || password.length() <= 0) {
                writeException("密码不能为空");
                return;
            }
            if (newPassword == null || newPassword.length() <= 0) {
                writeException("密码不能为空");
                return;
            }
            String hql = "select a.password from com.hiyo.hcf.model.Account a where " +
                    "a.id = '" + accountId + "'";

            List list = accountService.search(hql);
            if (list.size() > 0) {
                String pwd = (String) list.get(0);
                if (pwd.equals(password)) {
                    Account account = accountService.searchByKey(accountId);
                    account.setPassword(newPassword);
                    accountService.update(account);
                    writeSuccess();
                } else {
                    writeException("原密码输入不正确,重新输入");
                    return;
                }
            }
        } else {
            writeException("没有此版本接口");
        }
    }

    //找回密码
    @RequestMapping(value = "/api/account/resetPwd")
    public void resetPwd(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(value = "mobile", required = true) String mobile,
                         @RequestParam(value = "password", required = true) String password,
                         @RequestParam(value = "version", required = true) String version,
                         @RequestParam(value = "smsCode", required = true) String smsCode,
                         @RequestParam(value = "confirmPassword", required = true) String confirmPassword
    ) {

        if ("1.0.0".equals(version)) {
            //验证这个手机号有没有注册过 注册过进行下一步  没有则提示需要进行注册
            if (accountService.isMobileExist(mobile)) {
                //验证码是否正确
                //            boolean codeResult = mobileMsgService.checkVerifyCode(mobile, smsCode);
                if (!redisTemplate.hasKey(mobile)) {
                    writeException("验证码失效");
                    return;
                }

                String smsCode0 = redisTemplate.opsForValue().get(mobile).toString();
                boolean codeResult = smsCode0.equals(smsCode);
                if (codeResult) {
                    //两次输入的密码是否一样
                    if (password.equals(confirmPassword)) {
                        //修改密码
                        String hql = "select a from com.hiyo.hcf.model.Account a where " +
                                " a.mobile = '" + mobile + "'";
                        List l = accountService.search(hql);
                        if (l.size() > 0) {
                            Account account = (Account) l.get(0);
                            account.setPassword(password);
                            accountService.update(account);
                            writeSuccess();
                        } else {
                            writeException("修改失败");
                        }
                    } else {
                        writeException("两次输入的密码不一致,请重新设置密码");
                    }
                } else {
                    writeException("验证码错误");
                }
            } else {
                writeException("该手机号没有注册过,请到注册页面进行注册");
            }
        } else {
            writeException("没有此版本接口");
        }
    }
}
