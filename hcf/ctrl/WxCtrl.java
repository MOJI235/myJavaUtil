package com.hiyo.hcf.ctrl;

import com.hiyo.hcf.Constants;
import com.hiyo.hcf.model.Account;
import com.hiyo.hcf.model.Oem;
import com.hiyo.hcf.service.AccountService;
import com.hiyo.hcf.service.IWxService;
import com.hiyo.hcf.service.OemService;
import com.hiyo.hcf.util.TokenUtil;
import com.hiyo.hcf.util.WxHelper;
import com.tencent.common.Signature;
import com.tencent.common.Util;
import com.tencent.common.report.service.UnifiedOrderingService;
import com.tencent.protocol.ordering_protocol.UnifiedOrderReqData;
import com.tencent.protocol.ordering_protocol.UnifiedOrderResData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dswf.controller.BaseController;
import org.dswf.util.S;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;


@Controller
@RequestMapping("/api/wx/")
public class WxCtrl extends BaseController {
    private Log logger = LogFactory.getLog(WxCtrl.class);
    @Resource
    private AccountService accountService;
    @Resource
    private IWxService wxService;
    @Resource
    private OemService oemService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 登录时要带上的参数：
     * oemCode: OEM的编码
     * redirectUrl: 认证成功后跳转的地址，相对路径
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/oauth", method = RequestMethod.GET)
    public void oauth(HttpServletRequest request, HttpServletResponse response) {
        String oemCode = request.getParameter(Constants.OEMCODE);
        if (S.isEmpty(oemCode)) {
            logger.info("oemCode is null: uri=" + request.getRequestURI() + ",query=" + request.getQueryString());
            goError("OEM信息为空", response);
            return;
        }
        try {
            Oem oem = oemService.getOemByCode(oemCode);
            String url = oem.getHost() + "/api/wx/oauth2.do";
            if (request.getQueryString() != null) {
                url = url + "?" + request.getQueryString();
            }
            String authUrl = wxService.getAuthUrl(oemCode,url);
            logger.info("oauth redirect: " + authUrl);
            response.sendRedirect(authUrl);
        } catch (Exception e) {
            logger.error("", e);
        }
    }

//    private void goPage(String token, String path, HttpServletResponse response) {
//        try {
//            long accountId = TokenUtil.getIdByToken(token);
//            AccountProfile accountProfile = accountProfileService.getProfileByAccount(accountId);
//            AccountLevel accountLevel = accountProfile.getAccountLevel();
//            if (S.isEmpty(path)) {
//                if (S.isNotEmpty(accountLevel.getDefaultPath())) {
//                    path = accountLevel.getDefaultPath();
//                } else {
//                    path = "/m/home";
//                }
//            } else {
//                path = URLDecoder.decode(path, "utf-8");
//            }
//            String url = Constants.DEFAULT_URL + "#" + path;
//            logger.info("send redirect: " + url);
//            response.sendRedirect(url);
//        } catch (Exception e) {
//            logger.error("", e);
//        }
//    }

    @RequestMapping(value = "/oauth2", method = RequestMethod.GET)
    public void oauth2(HttpServletRequest request, HttpServletResponse response) {
        try {
            String code = request.getParameter("code");

            if (!StringUtils.isBlank(code)) {
//                Oem oem = oemService.getOemByCode(oemCode);
//                wxService.loginSuccess(oem,code);

//                AccountProfile accountProfile = accountProfileService.createAccountProfileByParent(oem,
//                        parentId,
//                        openId,
//                        nickname,
//                        headimgurl);
//                String token = StringUtil.getUUID();
//                redisClientTemplate.set(token, accountProfile.getAccount().getId() + "", com.hiyo.hcf.util.Constants.TOKEN_EXPIRE);

//                goPage(token, path, response);
                String redirectUrl = request.getParameter("redirectUrl");
                if (S.isEmpty(redirectUrl)) {
                    writeResponse("redirect url is null");
                    return;
                }
                if (!redirectUrl.contains("?")) {
                    redirectUrl = redirectUrl + "?t=" + System.currentTimeMillis();
                }
                redirectUrl = redirectUrl + "&code=" + code + "&" + Constants.OEMCODE + "=" + request.getParameter(Constants.OEMCODE);
                //request.getRequestDispatcher(redirectUrl).forward(request, response);
                logger.info("oauth2 redirect: " + redirectUrl);
                response.sendRedirect(redirectUrl);
            } else {
                goError("CODE为空", response);
            }
        } catch (Exception e) {
            logger.error("", e);
            goError("系统错误:" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), response);
        }
    }

    private void goError(String error, HttpServletResponse response) {
        try {
//            if ("no_subscribe".equals(error)
//                    && !S.isNotEmpty(openId)) {
//                JSONObject json = WxHelper.getInstance(oem).getWxUserInfoByOpenId(openId);
//                if (json != null && "1".equals(json.getString("subscribe"))) {
//                    goPage(request, response);
//                    return;
//                }
//            }
            String url = "/html/error.html?t=" + System.currentTimeMillis();
            if (S.isNotEmpty(error)) {
                url = url + "&error=" + error;
            }
//            if (S.isNotEmpty(request.getQueryString())) {
//                errorUrl = (errorUrl + "&" + request.getQueryString());
//            }
            response.sendRedirect(url);
        } catch (Exception e1) {
            logger.error("", e1);
        }
    }

    @RequestMapping(value = "/api", method = RequestMethod.GET)
    public void validate(HttpServletRequest req, HttpServletResponse resp) {
        try {
            boolean validate = validate(req);
            if (validate) {
                String echostr = req.getParameter("echostr");
                resp.getWriter().write(echostr);
                resp.getWriter().close();
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @RequestMapping(value = "/payNotify", method = RequestMethod.POST)
    public void payNotify(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.setContentType("text/xml;charset=utf-8");
            Element root = new SAXBuilder().build(req.getInputStream()).getRootElement();
            String return_code = root.getChildText("return_code");
            String return_msg = root.getChildText("return_msg");
            logger.info("pay result notification: return_code:" + return_code + ",return_msg=" + return_msg);
            Element resEle = new Element("xml");

            if ("SUCCESS".equals(return_code)) {
                String appid = root.getChildText("appid");
                Oem oem = oemService.getOemByWxAppId(appid);
                String mch_id = root.getChildText("mch_id");
                String nonce_str = root.getChildText("nonce_str");
                String sign = root.getChildText("sign");
                String result_code = root.getChildText("result_code");
                String err_code = root.getChildText("err_code");
                String err_code_des = root.getChildText("err_code_des");
                String openid = root.getChildText("openid");
                String bank_type = root.getChildText("bank_type");
                int total_fee = Integer.parseInt(root.getChildText("total_fee"));
                String transaction_id = root.getChildText("transaction_id");//微信支付订单号
                String out_trade_no = root.getChildText("out_trade_no");//商户系统的订单号
                String time_end = root.getChildText("time_end"); //支付完成时间
                logger.info("out_trade_no:" + out_trade_no + ",total_fee=" + total_fee + ",time_end=" + time_end + ",result_code=" + result_code + ",openid=" + openid + ",transaction_id=" + transaction_id);
                if (appid.equals(oem.getWxAppId())
                        && mch_id.equals(oem.getWxMchId()) && "SUCCESS".equals(result_code)) {
                    boolean success = wxService.payNotify(appid,
                            mch_id, result_code, err_code, err_code_des, openid, total_fee, transaction_id, out_trade_no, time_end);
                    if (success) {
                        resEle.addContent(new Element("return_code").addContent(new CDATA("SUCCESS")));
                        resEle.addContent(new Element("return_msg").addContent(new CDATA("OK")));
                    } else {
                        resEle.addContent(new Element("return_code").addContent(new CDATA("FAIL")));
                        resEle.addContent(new Element("return_msg").addContent(new CDATA("参数校验错误或业务状态接收失败")));
                    }
                } else {
                    resEle.addContent(new Element("return_code").addContent(new CDATA("FAIL")));
                    resEle.addContent(new Element("return_msg").addContent(new CDATA("参数校验错误")));
                }
            } else {
                resEle.addContent(new Element("return_code").addContent(new CDATA("FAIL")));
                resEle.addContent(new Element("return_msg").addContent(new CDATA("参数缺失")));
            }

            Format format = Format.getCompactFormat();
            format.setEncoding("utf-8");
            StringWriter sw = new StringWriter();
            XMLOutputter outputter = new XMLOutputter(format);
            outputter.output(resEle, sw);
            resp.getWriter().write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            resp.getWriter().write(sw.toString());
            resp.getWriter().close();
            logger.info(sw.toString());
        } catch (Exception e) {
            logger.error("支付通知处理失败：", e);
        }
    }

    @RequestMapping(value = "/jsconfig")
    public void jsconfig(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/xml;charset=utf-8");
        String url = req.getParameter("targetUrl");
        String token = req.getParameter("token");
        long accountId = TokenUtil.getIdByToken(token);
        Account account = accountService.searchByKey(accountId);
        Oem oem = account.getOem();
        logger.info("jsconfig: token=" + token + ",accountId=" + accountId + ",oem=" + oem);
        Map<String, String> map = new WxHelper(redisTemplate).sign(oem, url);
        JSONObject jsonObject = new JSONObject();
        for (String key : map.keySet()) {
            jsonObject.put(key, map.get(key));
        }
        jsonObject.put("appid", oem.getWxAppId());
        try {
            resp.getWriter().write(jsonObject.toString());
            resp.getWriter().close();
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    @RequestMapping(value = "/jsconfig2")
    public void jsconfig2(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/xml;charset=utf-8");
        String url = req.getParameter("targetUrl");
//        String token = req.getParameter("token");
//        long accountId = TokenUtil.getIdByToken(token);
//        Account account = accountService.searchByKey(accountId);
        Oem oem = oemService.searchAll().get(0);
//        logger.info("jsconfig: token=" + token + ",accountId=" + accountId + ",oem=" + oem);
        Map<String, String> map = new WxHelper(redisTemplate).sign(oem, url);
        JSONObject jsonObject = new JSONObject();
        for (String key : map.keySet()) {
            jsonObject.put(key, map.get(key));
        }
        jsonObject.put("appid", oem.getWxAppId());
        try {
            resp.getWriter().write(jsonObject.toString());
            resp.getWriter().close();
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    /**
     * @param request
     * @param response
     */
    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    public void pay(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.setHeader("Content-type", "text/html;charset=UTF-8");
            JSONObject jsonObject = getRequestJsonObj();
            String token = request.getParameter("token");
            if (S.isEmpty(token)) {
                writeException("no token!");
                return;
            }
            Long accountId = TokenUtil.getIdByToken(token);
            Account account = accountService.searchByKey(accountId);
            String oemCode = account.getOem().getCode();
            String openId = TokenUtil.getOpenIdByToken(token);
            String orderNo = jsonObject.getString("orderNo");


            JSONObject map = new JSONObject(); //for return
            int totalPayAmount = wxService.getPayTotalAmount(oemCode, orderNo);
            Oem oem = oemService.getOemByCode(oemCode);
            String wxAppId = oem.getWxAppId();
            String wxMchId = oem.getWxMchId();
            String wxIp = oem.getWxIp();
            String wxNotifyUrl = oem.getWxPayNotifyUrl();


            if (totalPayAmount > 0) {
                UnifiedOrderReqData orderReqData = new UnifiedOrderReqData(
                        wxAppId,
                        wxMchId,
                        oem.getWxKey(),
                        "JSAPI",
                        wxIp,
                        orderNo,
                        totalPayAmount,
                        orderNo,
                        openId,
                        wxNotifyUrl);
                //orderReqData.setNotify_url(Constants.NotifyUrl);
                String orderingServiceResponseString = null;
                try {
                    orderingServiceResponseString = UnifiedOrderingService.request(orderReqData);
                } catch (Exception e) {
                    logger.error("", e);
                }
                //logger.debug("orderingServiceResponseString:"+orderingServiceResponseString);
                //将从API返回的XML数据映射到Java对象
                UnifiedOrderResData orderingResData = (UnifiedOrderResData) Util.getObjectFromXML(orderingServiceResponseString, UnifiedOrderResData.class);
                logger.info("UnifiedOrderResData: " + jsonBinder.toJson(orderingResData));
                if (orderingResData.getReturn_code().equals("SUCCESS") && orderingResData.getResult_code().equals("SUCCESS")) {
                    map.put("appId", orderReqData.getAppid());
                    Long timeStamp = new Date().getTime() / 1000;
                    map.put("timeStamp", timeStamp.toString());
                    map.put("nonceStr", orderingResData.getNonce_str());
                    String packageParam = "prepay_id=" + orderingResData.getPrepay_id();
                    map.put("package", packageParam);
                    map.put("signType", "MD5");
                    String paySign = Signature.getSign(oem.getWxKey(), map.map);
                    map.put("paySign", paySign);
                    map.put("flag", true);
                    map.put("msg", "订单提交成功！");
                } else {
                    map.put("flag", false);
                    map.put("msg", "微信支付失败");
                }
            } else {
                map.put("flag", false);
                map.put("msg", "微信支付失败");
            }
            logger.info("pay return: " + map.toString());

            writeResponse(map);
        } catch (Exception e) {
            logger.error("", e);
            writeException(e);
        }
    }

    @RequestMapping(value = "/qrcode", method = RequestMethod.POST)
    public void qrcode(HttpServletRequest request, HttpServletResponse response) {
        JSONObject jsonObject = getRequestJsonObj();
        boolean infinite = jsonObject.getBoolean("infinite");
        String qrscene = jsonObject.getString("qrscene");
        String oemCode = jsonObject.getString(Constants.OEMCODE);
        if (S.isEmpty(oemCode)) {
            writeException("no oemCode!");
            return;
        }
        Oem oem = oemService.getOemByCode(oemCode);
        try {
            JSONObject obj = new JSONObject();
            obj.put("success", true);
            obj.put("qrcode", new WxHelper(redisTemplate)
                    .getQrCode(oem, qrscene, infinite));
            writeResponse(obj);
        } catch (Exception e) {
            logger.error("", e);
            writeException(e);
        }
    }

    @RequestMapping(value = "/api", method = RequestMethod.POST)
    public void api(HttpServletRequest req, HttpServletResponse resp) {
        if (!validate(req)) {
            logger.error("invalid request: " + req.getQueryString());
            return;
        }
        resp.setContentType("text/xml;charset=utf-8");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream(), "utf-8"));
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line);
            }
            br.close();
            logger.info(sb.toString());
            final String oc = req.getParameter(Constants.OEMCODE);
            final Element reqEle = new SAXBuilder().build(new StringReader(sb.toString())).getRootElement();
            Element resEle = null;
            final List l = new ArrayList();
            Thread t = new Thread() {

                public void run() {
                    try {
                        Element api = wxService.api(oc, reqEle);
                        l.add(api);
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }
            };
            t.start();
            t.join(3000);
            if (l.size() > 0) {
                resEle = (Element) l.get(0);
            }
            if (resEle == null) {
                resEle = wxService.getDefaultReply(oc, reqEle);
            }
            if (resEle != null) {
                Format format = Format.getRawFormat();
                format.setEncoding("utf-8");
                StringWriter sw = new StringWriter();
                XMLOutputter outputter = new XMLOutputter(format);
                outputter.output(resEle, sw);
                resp.getWriter().write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                resp.getWriter().write(sw.toString());
                resp.getWriter().close();
                logger.info("resp: " + sw.toString());
            } else {
                resp.getWriter().write("");
                resp.getWriter().close();
                logger.info("resp: is null");
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }


    private boolean validate(HttpServletRequest req) {
        String signature = req.getParameter("signature");
        String timestamp = req.getParameter("timestamp");
        String nonce = req.getParameter("nonce");
        String token = req.getParameter(Constants.OEMCODE);
        if (S.isEmpty(token)) {
            return false;
        }
        Oem oem = oemService.getOemByCode(token);
        if (nonce == null
                || timestamp == null
                || signature == null) {
            return false;
        }
        List list = new ArrayList();
        list.add(token);
        list.add(timestamp);
        list.add(nonce);
        Collections.sort(list);
        String s = "";
        for (int i = 0; i < list.size(); i++) {
            String _s = (String) list.get(i);
            s = s + _s;
        }
        logger.info("str=" + s + ",signature:" + signature);
        if (new WxHelper(redisTemplate).encode("SHA1", s).equalsIgnoreCase(signature)) {
//            resp.getWriter().write(echostr);
//            resp.getWriter().close();
            return true;
        } else {
            return false;
        }
    }

}
