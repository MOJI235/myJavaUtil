package com.hiyo.hcf.dao;

import com.hiyo.hcf.model.Balance;
import org.dswf.dao.BaseDao;
import org.springframework.stereotype.Repository;

@Repository("balanceDao")
public class BalanceDao extends BaseDao<Balance,Long> implements IBalanceDao{

    public Class<Balance> getClazz() {
        return Balance.class;
    }
}