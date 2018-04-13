package com.hiyo.hcf.dao;

import com.hiyo.hcf.model.*;
import org.dswf.dao.BaseDao;
import org.springframework.stereotype.Repository;

@Repository("hcfBaseDao")
public class HcfBaseDao extends BaseDao<HcfBase,Long> implements IHcfBaseDao{

    public Class<HcfBase> getClazz() {
        return HcfBase.class;
    }
}