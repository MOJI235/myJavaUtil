package com.hiyo.hcf.dao;

import com.hiyo.hcf.model.ChargeRecord;
import org.dswf.dao.BaseDao;
import org.springframework.stereotype.Repository;

@Repository("chargeRecordDao")
public class ChargeRecordDao extends BaseDao<ChargeRecord,Long> implements IChargeRecordDao{

    public Class<ChargeRecord> getClazz() {
        return ChargeRecord.class;
    }
}