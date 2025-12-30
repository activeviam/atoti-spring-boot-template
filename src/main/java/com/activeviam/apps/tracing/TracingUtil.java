/*
 * Copyright (C) ActiveViam 2025
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.activeviam.apps.tracing;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;

import com.activeviam.tech.core.internal.observability.Observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.Builder;
import lombok.NonNull;

/**
 * Simple tracing utility to create spans.
 * <p>
 * Spans created here can be auto-closed when used with Lombok's {@link lombok.Cleanup} annotation:
 * <pre>
 *     {@code
 *     myMethod() {
 *       @Cleanup var closeableSpan = TracingUtil.startSpan();
 *       ... method body ...
 *       ... Span is automatically closed here ...
 *     }
 *     }
 * </pre>
 * This span will be automatically closed when the variable goes out of scope.
 *
 * @author ActiveViam
 */
public class TracingUtil {

    public static String defaultTracingScopeName = "Atoti-Application";

    public static String getDefaultTracingScopeName() {
        return defaultTracingScopeName;
    }

    public static void setDefaultTracingScopeName(String defaultTracingScopeName) {
        TracingUtil.defaultTracingScopeName = defaultTracingScopeName;
    }
    
    /**
     * Wrapper around a span and its scope. This is so we can close the scope and end the span at the same time.
     * This allows easy auto-closing of spans when used with Lombok's @Cleanup annotation.
     * <pre>
     *     {@code
     *     @Cleanup var closeableSpan = TracingUtil.startSpan();
     *     }
     * </pre>
     */
    public record CloseableSpan(Span span, Scope scope) implements Closeable {
        @Override
        public void close() {
            scope.close();
            span.end();
        }
    }

    public static Tracer getTracer(@NonNull String tracerScopeName){
        return Observability.getEffectiveOtelInstance().getTracer(tracerScopeName);
    }

    /// Starts a span with the default tracing scope name and a span name based on the caller method name.
    public static TracingUtil.CloseableSpan startSpan(){
        return closeableSpanBuilder().build();
    }

    /// Starts a span with the default tracing scope name and the given span name.
    public static TracingUtil.CloseableSpan startSpan(@NonNull String spanName){
        return closeableSpanBuilder()
                .spanName(spanName)
                .build();
    }

    /// Starts a span with the default tracing scope name, the given span name., and the given attributes.
    public static TracingUtil.CloseableSpan startSpan(@Nullable String spanName, @Nullable Attributes attributes){
        return closeableSpanBuilder()
                .spanName(spanName)
                .attributes(attributes)
                .build();
    }

    public static TracingUtil.CloseableSpanBuilder closeableSpanBuilder(){
        return new TracingUtil.CloseableSpanBuilder();
    }

    /**
     * Starts a span with the given tracer scope name, span name, and attributes.
     * <br>
     * Can use {@link #closeableSpanBuilder()} to build the span with a builder pattern.
     * 
     * @param tracerScopeName Name of the Tracer scope. If null, uses the default tracing scope name.
     * @param spanName Name of the span. If null, uses the caller method name.
     * @param attributes Attributes to set on the span. If null, uses empty attributes.
     * @return The started span wrapped in a CloseableSpan.
     */
    @Builder
    protected static TracingUtil.CloseableSpan startSpan(
            @Nullable String tracerScopeName, 
            @Nullable String spanName, 
            @Nullable Attributes attributes){
        tracerScopeName = tracerScopeName == null ? getDefaultTracingScopeName() : tracerScopeName;
        spanName = spanName == null ? CallerTrace.ofExternalCaller().simpleName() : spanName;
        attributes = attributes == null ? Attributes.empty() : attributes;

        var span = getTracer(tracerScopeName)
                .spanBuilder(spanName)
                .setAllAttributes(attributes)
                .startSpan();
        var scope = span.makeCurrent();
        return new TracingUtil.CloseableSpan(span, scope);
    }

    /// Simple record to hold caller's stack trace information
    protected record CallerTrace(String className, String methodName, StackTraceElement trace) {

        /**
         * Creates a CallerTrace for the method that called a TracingUtil method.
         * @return the first non-TracingUtil caller method name
         */
        public static TracingUtil.CallerTrace ofExternalCaller(){
            var stackTrace = Thread.currentThread().getStackTrace();
            // Start from index 1 to skip the getStackTrace method itself
            for (int i = 1; i < stackTrace.length; i++) {
                var trace = stackTrace[i];
                // Skip TracingUtil class methods
                if(!trace.getClassName().startsWith(TracingUtil.class.getName())){
                    return TracingUtil.CallerTrace.of(trace);
                }
            }
            throw new IllegalStateException("Could not find caller method name for stack trace: " + Arrays.stream(Thread.currentThread().getStackTrace()).map(Object::toString).toList());
        }

        public static TracingUtil.CallerTrace of(StackTraceElement trace){
            var className = List.of(trace.getClassName().split("\\.")).getLast();
            var methodName = trace.getMethodName();
            return new TracingUtil.CallerTrace(className, methodName, trace);
        }
        public String simpleName(){
            return className + "." + methodName;
        }
    }
}

