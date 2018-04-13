package com.hiyo.hcf.ctrl;

import com.hiyo.hcf.exception.HandleExceptionController;
import com.hiyo.hcf.model.Account;
import com.hiyo.hcf.model.ChargeRecord;
import com.hiyo.hcf.service.AccountService;
import com.hiyo.hcf.service.BalanceService;
import com.hiyo.hcf.service.ChargeRecordService;
import com.hiyo.hcf.service.MobileMsgService;
import com.hiyo.hcf.util.TokenUtil;
import org.dswf.util.ArithUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by dishi on 2017/3/23.
 */
@Controller
public class ChargeCtrl extends HandleExceptionController {
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


    @RequestMapping(value = "/api/charge", method = RequestMethod.POST)
    public void charge(HttpServletRequest request,
                       HttpServletResponse response,
                       @RequestParam(value = "token", required = true) String token,
                       @RequestParam(value = "version", required = true) String version) {

    }


    @RequestMapping(value = "/api/charge/confirm", method = RequestMethod.POST)
    public void orderConfirm(HttpServletRequest request,
                             HttpServletResponse response,
                             @RequestParam(value = "token", required = true) String token,
                             @RequestParam(value = "version", required = true) String version) {
        final Integer amount = (int) ArithUtil.mul(getRequestJsonObj().getDouble("amount"), 100);
        String payMethod = getRequestJsonObj().getString("payMethod");
        Long accountId = TokenUtil.getIdByToken(token);
        final Account account = accountService.searchByKey(accountId);

        ChargeRecord chargeRecord = new ChargeRecord();
        chargeRecord.setAccount(account);
        //    chargeRecord.setCardNum(UUID.randomUUID().toString());
        chargeRecord.setAmount(amount);
        chargeRecord.setPayMethod(payMethod);
        chargeRecord.setStatus(ChargeRecord.status_to_pay);
        chargeRecord.setBizId(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + (1000 + new Random().nextInt(8999)));
        chargeRecordService.save(chargeRecord);

        //todo 返回支付需要的必要参数
        writeEntity(chargeRecord);
    }


    @RequestMapping(value = "/api/charge/list", method = RequestMethod.POST)
    public void chargeRecordList(HttpServletRequest request,
                                 HttpServletResponse response,
                                 @RequestParam(value = "token", required = true) String token,
                                 @RequestParam(value = "version", required = true) String version) {
        String hql = "select a from com.hiyo.hcf.model.ChargeRecord a where" +
                " a.account.id = " + TokenUtil.getIdByToken(token) + "" +
                " and" +
                " a.status = '" + ChargeRecord.status_paid + "'";
        List list = chargeRecordService.search(hql);
        writeResponse(list);
    }

}
