package com.hiyo.hcf.ctrl;


import com.hiyo.hcf.exception.HandleExceptionController;
import com.hiyo.hcf.model.Balance;
import com.hiyo.hcf.service.AccountService;
import com.hiyo.hcf.service.BalanceService;
import com.hiyo.hcf.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 账户余额有关的controller
 */
@Controller
@RequestMapping("/api/balance")
public class BalanceCtrl extends HandleExceptionController {
    @Resource
    private AccountService accountService;
    @Resource
    private BalanceService balanceService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 开启账户
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/open")
    public void open(HttpServletRequest request, HttpServletResponse response
    ) {
        String token = getRequestJsonObj().getString("token");
        String payPassword = getRequestJsonObj().getString("payPassword");
        ValueOperations<String,Long> vo = redisTemplate.opsForValue();
        Long accountId = vo.get(token);
        if (accountId == -1) {
            writeException("token失效,请重新登录");
            return;
        }
        String hql = "select a from com.hiyo.hcf.model.Balance a where a.account.id = " + accountId;
        Balance balance = (Balance) balanceService.search(hql).get(0);
        balance.setBalanceStatus(Balance.balanceStatus_on);
        if (payPassword.length() != 6 || !StringUtil.isNumeric(payPassword)) {
            writeException("支付密码不合法");
            return;
        }
        balance.setPayPassword(payPassword);
        balanceService.update(balance);
        writeSuccess();
    }

    /**
     * 获取账户信息
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/get")
    public void get(HttpServletRequest request, HttpServletResponse response) {
        String token = getRequestJsonObj().getString("token");
        String payPassword = getRequestJsonObj().getString("payPassword");

        Long accountId = Long.parseLong(redisTemplate.opsForValue().get(token).toString());
        if (accountId == -1) {
            writeException("token失效,请重新登录");
            return;
        }
        String hql = "select a from com.hiyo.hcf.model.Balance a where a.account.id = " + accountId;
        Balance balance = (Balance) balanceService.search(hql).get(0);
        writeEntity(balance);
    }

}

