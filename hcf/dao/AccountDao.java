package com.hiyo.hcf.dao;

import com.hiyo.hcf.model.*;
import org.dswf.dao.BaseDao;
import org.springframework.stereotype.Repository;

@Repository("accountDao")
public class AccountDao extends BaseDao<Account,Long> implements IAccountDao{

    public Class<Account> getClazz() {
        return Account.class;
    }
}