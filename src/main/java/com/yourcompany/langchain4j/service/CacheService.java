package com.yourcompany.langchain4j.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 * 提供统一的缓存操作接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取缓存失败: {}", key, e);
            return null;
        }
    }
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        set(key, value, 1, TimeUnit.HOURS);
    }
    
    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存失败: {}", key, e);
        }
    }
    
    /**
     * 删除缓存
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 批量删除缓存
     */
    public Long delete(Collection<String> keys) {
        try {
            return redisTemplate.delete(keys);
        } catch (Exception e) {
            log.error("批量删除缓存失败", e);
            return 0L;
        }
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查缓存失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
        } catch (Exception e) {
            log.error("设置过期时间失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 获取过期时间
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("获取过期时间失败: {}", key, e);
            return -1L;
        }
    }
    
    /**
     * 哈希 - 获取
     */
    public Object hashGet(String key, String field) {
        try {
            return redisTemplate.opsForHash().get(key, field);
        } catch (Exception e) {
            log.error("哈希获取失败: {}.{}", key, field, e);
            return null;
        }
    }
    
    /**
     * 哈希 - 设置
     */
    public void hashSet(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
        } catch (Exception e) {
            log.error("哈希设置失败: {}.{}", key, field, e);
        }
    }
    
    /**
     * 哈希 - 批量设置
     */
    public void hashSetAll(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
        } catch (Exception e) {
            log.error("哈希批量设置失败: {}", key, e);
        }
    }
    
    /**
     * 列表 - 右侧推入
     */
    public Long listRightPush(String key, Object value) {
        try {
            return redisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.error("列表推入失败: {}", key, e);
            return 0L;
        }
    }
    
    /**
     * 列表 - 获取范围
     */
    public List<Object> listRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("列表获取范围失败: {}", key, e);
            return null;
        }
    }
    
    /**
     * 列表 - 获取大小
     */
    public Long listSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            log.error("列表获取大小失败: {}", key, e);
            return 0L;
        }
    }
    
    /**
     * 递增
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("递增失败: {}", key, e);
            return 0L;
        }
    }
    
    /**
     * 递减
     */
    public Long decrement(String key, long delta) {
        try {
            return increment(key, -delta);
        } catch (Exception e) {
            log.error("递减失败: {}", key, e);
            return 0L;
        }
    }
}
