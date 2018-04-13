package com.hiyo.hcf.controller;

import com.hiyo.hcf.model.HcfBase;
import com.hiyo.hcf.service.*;
import org.dswf.controller.BaseController;
import org.dswf.dao.SortParam;
import org.dswf.service.IBaseService;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.dswf.model.IBaseEntity;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Controller
@RequestMapping("/hcfBase")
public class HcfBaseController extends BaseController {
@Resource
private AccountService accountService;
@Resource
private BalanceService balanceService;
@Resource
private HcfBaseService hcfBaseService;
@Resource
private MobileMsgService mobileMsgService;
@Resource
private OemService oemService;


    @ModelAttribute
    public void populateModel(HttpServletRequest request, Model model) {
        model.addAttribute("hcfBase", parseRequest( HcfBase.class));
    }


@RequestMapping(value = "/moveUp")
public void moveUp(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
try {
moveUp(
hcfBaseService,
hcfBase,
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
public void moveDown(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
try {
moveDown(
hcfBaseService,
hcfBase,
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
    public void add(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
                JSONObject jsonObject = add(request, response, hcfBaseService, hcfBase, result);

        writeResponse( jsonObject);
    }


    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    public void modify(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
        JSONObject jsonObject = modify(request, response, hcfBaseService, hcfBase, result);
        writeResponse( jsonObject);
    }


    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public void search(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
        //result.addError(new FieldError("hcfBase","serialNo",null,false,new String[]{"NotNull"},null,"error msg"));
                JSONObject jsonObject = search(request, response, hcfBaseService, hcfBase, result);
        
        writeResponse( jsonObject);
    }

    @RequestMapping(value = "/toModify", method = RequestMethod.POST)
    public void toModify(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
        JSONObject jsonObject = toModify(request, response, hcfBaseService, hcfBase, result);

appendOptions(
"oem",
    oemService.search("select a from com.hiyo.hcf.model.Oem a where 1=1 order by a.createTime desc", 0, 20),
jsonObject.getJSONObject("entity"),
new AppendOptionCallback() {

public String getLabel(IBaseEntity entity) {
return entity.getName();
}
}
);



writeResponse( jsonObject);
    }


    @RequestMapping(value = "/toAdd", method = RequestMethod.POST)
    public void toAdd(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
        JSONObject jsonObject = toAdd(request, response, hcfBaseService, hcfBase, result);

appendOptions(
"oem",
    oemService.search("select a from com.hiyo.hcf.model.Oem a where 1=1 order by a.createTime desc", 0, 20),
jsonObject.getJSONObject("entity"),
new AppendOptionCallback() {

public String getLabel(IBaseEntity entity) {
return entity.getName();
}
}
);



writeResponse( jsonObject);
    }

    @RequestMapping(value = "/toSearch", method = RequestMethod.POST)
    public void toSearch(HttpServletRequest request, HttpServletResponse response, @Valid @ModelAttribute("hcfBase") HcfBase hcfBase, BindingResult result) {
        JSONObject jsonObject = toSearch(request, response, hcfBaseService, hcfBase, result);

appendOptions(
"oem",
    oemService.search("select a from com.hiyo.hcf.model.Oem a where 1=1 order by a.createTime desc", 0, 20),
jsonObject.getJSONObject("entity"),
new AppendOptionCallback() {

public String getLabel(IBaseEntity entity) {
return entity.getName();
}
}
);



writeResponse( jsonObject);
    }

    @RequestMapping(value = "/deleteAll", method = RequestMethod.POST)
    public void deleteAll(HttpServletRequest request, HttpServletResponse response) {
        JSONObject jsonObject = deleteAll(request, response, hcfBaseService);
        writeResponse( jsonObject);
    }

}
