package com.hiyo.hcf.dao;

import com.hiyo.hcf.model.MobileMsg;
import org.dswf.dao.BaseDao;
import org.springframework.stereotype.Repository;

@Repository("mobileMsgDao")
public class MobileMsgDao extends BaseDao<MobileMsg,Long> implements IMobileMsgDao{

    public Class<MobileMsg> getClazz() {
        return MobileMsg.class;
    }
}