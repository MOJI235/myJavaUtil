package com.hiyo.hcf.util;

import com.hiyo.hcf.Constants;
import com.hiyo.hcf.exception.Error;
import com.hiyo.hcf.exception.ErrorCode;
import com.hiyo.hcf.exception.ErrorCodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by wangshuo on 17/1/23.
 */
@Component
public class TokenUtil {

    @Autowired
    private RedisTemplate redisTemplate;


    private static TokenUtil tokenUtil;

    @PostConstruct
    public void init() {
        tokenUtil = this;
        tokenUtil.redisTemplate = this.redisTemplate;
    }


    public static long getIdByToken(String token) {
        boolean result = tokenUtil.redisTemplate.hasKey(token);
        if (result) {
            return Long.parseLong(tokenUtil.redisTemplate.opsForValue().get(token).toString());
        } else {
            throw new ErrorCodeException(new Error(ErrorCode.TOKEN_EXPIRES));
        }
    }

    public static String getOpenIdByToken(String token) {
        String key = token + "." + Constants.OPENID;
        boolean result = tokenUtil.redisTemplate.hasKey(key);
        if (result) {
            return tokenUtil.redisTemplate.opsForValue().get(key).toString();
        } else {
            throw new ErrorCodeException(new Error(ErrorCode.TOKEN_EXPIRES));
        }
    }

    public static String getOemCodeByToken(String token) {
        String key = token + "." + Constants.OEMCODE;
        boolean result = tokenUtil.redisTemplate.hasKey(key);
        if (result) {
            return tokenUtil.redisTemplate.opsForValue().get(key).toString();
        } else {
            throw new ErrorCodeException(new Error(ErrorCode.TOKEN_EXPIRES));
        }
    }
}
