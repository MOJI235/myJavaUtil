package com.hiyo.hcf.controller;

import com.hiyo.hcf.exception.HandleExceptionController;
import com.hiyo.hcf.model.Account;
import com.hiyo.hcf.service.*;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping("/account")
public class AccountController extends HandleExceptionController {
    @Resource
    private AccountService accountService;


    @ModelAttribute
    public void populateModel(HttpServletRequest request, Model model) {
        model.addAttribute("account", parseRequest(Account.class));
    }


    @RequestMapping(value = "/moveUp")
    public void moveUp(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        try {
            moveUp(
                    accountService,
                    account,
                    "orderIndex",
                    "and 1=1"
            );
            writeSuccess();
        } catch (Exception e) {
            logger.error("", e);
            writeException(e);
        }
    }

    @RequestMapping(value = "/moveDown")
    public void moveDown(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        try {
            moveDown(
                    accountService,
                    account,
                    "orderIndex",
                    "and 1=1"
            );
            writeSuccess();
        } catch (Exception e) {
            logger.error("", e);
            writeException(e);
        }
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public void add(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        JSONObject jsonObject = add(request, response, accountService, account, result);

        writeResponse(jsonObject);
    }


    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    public void modify(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        JSONObject jsonObject = modify(request, response, accountService, account, result);
        writeResponse(jsonObject);
    }


    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public void search(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        //result.addError(new FieldError("account","serialNo",null,false,new String[]{"NotNull"},null,"error msg"));
        JSONObject jsonObject = search(request, response, accountService, account, result);

        writeResponse(jsonObject);
    }

    @RequestMapping(value = "/toModify", method = RequestMethod.POST)
    public void toModify(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        JSONObject jsonObject = toModify(request, response, accountService, account, result);


        writeResponse(jsonObject);
    }


    @RequestMapping(value = "/toAdd", method = RequestMethod.POST)
    public void toAdd(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        JSONObject jsonObject = toAdd(request, response, accountService, account, result);


        writeResponse(jsonObject);
    }

    @RequestMapping(value = "/toSearch", method = RequestMethod.POST)
    public void toSearch(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("account") Account account, BindingResult result) {
        JSONObject jsonObject = toSearch(request, response, accountService, account, result);


        writeResponse(jsonObject);
    }

    @RequestMapping(value = "/deleteAll", method = RequestMethod.POST)
    public void deleteAll(HttpServletRequest request, HttpServletResponse response) {
        JSONObject jsonObject = deleteAll(request, response, accountService);
        writeResponse(jsonObject);
    }


}
