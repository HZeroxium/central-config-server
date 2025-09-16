package com.example.rest.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Lightweight AOP timing for Thrift client adapter.
 * Measures execution time for all public methods in ThriftUserClientAdapter
 * and emits Micrometer timers with consistent tags.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ThriftClientTimingAspect {

    private final MeterRegistry meterRegistry;

    @Around("within(com.example.rest.user.adapter.thrift.ThriftUserClientAdapter) && execution(public * *(..))")
    public Object timeThriftClientCalls(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getSignature().getDeclaringTypeName();
        String methodName = pjp.getSignature().getName();

        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            outcome = "error";
            throw t;
        } finally {
            Timer timer = Timer
                    .builder("thrift.client.seconds")
                    .description("Latency of Thrift client calls from REST service")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("outcome", outcome)
                    .register(meterRegistry);
            sample.stop(timer);
            log.debug("Thrift client call timed: {}#{} outcome={}", className, methodName, outcome);
        }
    }
}


