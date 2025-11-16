package com.vng.zing.zcm.pingconfig.cache;

import com.vng.zing.zcm.pingconfig.ConfigHashCalculator;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * Custom key generator for config hash cache.
 * <p>
 * Generates cache keys based on the ConfigHashCalculator's environment context
 * (application name, profile, label) to ensure unique cache entries per
 * configuration context.
 * <p>
 * This class is registered as a bean in {@link ConfigHashCacheConfig}
 * to ensure it's available when cache configuration is enabled.
 */
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

