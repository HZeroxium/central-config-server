package com.vng.zing.zcm.pingconfig.cache;

import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Custom key generator for config hash cache.
 * <p>
 * Generates cache keys based on the ConfigHashCalculator's environment context
 * (application name, profile, label) to ensure unique cache entries per
 * configuration context.
 */
@Component("configHashCacheKeyGenerator")
public class ConfigHashCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        if (target instanceof ConfigHashCalculator calculator) {
            return calculator.getCacheKey();
        }
        // Fallback: use method name if target is not ConfigHashCalculator
        return method.getName();
    }
}

