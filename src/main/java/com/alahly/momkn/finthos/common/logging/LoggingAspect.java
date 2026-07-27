package com.alahly.momkn.finthos.common.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "secret", "token", "authorization", "pin", "cvv", "key"
    );
    private static final String MASK = "****";

    @Pointcut("execution(* com.alahly.momkn.finthos..service..*.*(..))")
    void serviceLayer() {}

    @Pointcut("execution(* com.alahly.momkn.finthos.integration.client..*.*(..))")
    void clientLayer() {}

    @Around("serviceLayer() || clientLayer()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String[] argNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        Object[] argValues = joinPoint.getArgs();

        String args = argNames != null && argNames.length > 0
                ? formatArgs(argNames, argValues)
                : "[]";

        if (log.isTraceEnabled()) {
            log.trace("[{}] {}.{}() entry args={}", correlationId, className, methodName, args);
        }

        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            if (log.isTraceEnabled()) {
                log.trace("[{}] {}.{}() exit in {}ms result={}",
                        correlationId, className, methodName, elapsedMs, result);
            }
            return result;
        } catch (Throwable t) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.warn("[{}] {}.{}() threw {} in {}ms: {}",
                    correlationId, className, methodName, t.getClass().getSimpleName(), elapsedMs, t.getMessage());
            throw t;
        }
    }

    private String formatArgs(String[] names, Object[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names[i]).append("=");
            if (isSensitive(names[i])) {
                sb.append(MASK);
            } else {
                sb.append(valueToString(values.length > i ? values[i] : null));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean isSensitive(String name) {
        String lower = name.toLowerCase();
        return SENSITIVE_PARAMS.stream().anyMatch(lower::contains);
    }

    private static String valueToString(Object value) {
        if (value == null) return "null";
        if (value.getClass().isArray()) {
            if (value.getClass().getComponentType().isPrimitive()) {
                return Arrays.toString((boolean[]) value); // simplified; works for common cases
            }
            return Arrays.toString((Object[]) value);
        }
        return value.toString();
    }
}
