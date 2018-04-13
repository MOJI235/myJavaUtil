package com.hiyo.hcf.dao;

import com.hiyo.hcf.model.Oem;
import org.dswf.dao.BaseDao;
import org.springframework.stereotype.Repository;

@Repository("oemDao")
public class OemDao extends BaseDao<Oem, Long> implements IOemDao {

    public Class<Oem> getClazz() {
        return Oem.class;
    }
}