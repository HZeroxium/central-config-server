package com.example.control.config.metrics.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark methods for custom profiling and metrics collection.
 * <p>
 * When applied to a method, it enables automatic collection of:
 * <ul>
 *   <li>Method execution time</li>
 *   <li>Exception count and types</li>
 *   <li>Custom tags and metadata</li>
 * </ul>
 * <p>
 * This annotation works in conjunction with {@link com.example.control.config.metrics.aspect.PerformanceMetricsAspect}
 * to provide comprehensive method-level profiling.
 * <p>
 * Example usage:
 * <pre>{@code
 * @Profiled(
 *     metricName = "custom_operation",
 *     tags = {"operation=process", "component=service"}
 * )
 * public void processData(String input) {
 *     // method implementation
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Profiled {

    /**
     * Custom metric name override.
     * <p>
     * If not specified, the metric name will be derived from the class and method name
     * in the format: {className}.{methodName}
     * 
     * @return custom metric name
     */
    String metricName() default "";

    /**
     * Additional tags to include with the metrics.
     * <p>
     * Tags should be provided in the format "key=value".
     * 
     * @return array of tag strings
     */
    String[] tags() default {};

    /**
     * Description for the metric.
     * <p>
     * This description will be included in the metric metadata
     * for better documentation and observability.
     * 
     * @return metric description
     */
    String description() default "";

    /**
     * Whether to record method parameters as tags.
     * <p>
     * Note: This may impact performance for methods with complex parameters.
     * 
     * @return true if parameters should be recorded as tags
     */
    boolean includeParameters() default false;

    /**
     * Whether to record method return value information.
     * <p>
     * This will add tags like "returnType" and potentially "returnValue"
     * for simple return types.
     * 
     * @return true if return value should be recorded
     */
    boolean includeReturnValue() default false;

    /**
     * Sampling rate for this specific method (0.0 to 1.0).
     * <p>
     * 1.0 = profile all calls, 0.5 = profile 50% of calls.
     * This allows fine-grained control over profiling overhead.
     * 
     * @return sampling rate
     */
    double samplingRate() default 1.0;
}
