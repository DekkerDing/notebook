package io.github.dekkerding.examples;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTimeAspect {

    @Around("execution(* io.github.dekkerding.examples.PortalController.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Long startTime = System.currentTimeMillis();

        Object proceed = joinPoint.proceed();

        Long endTime = System.currentTimeMillis();
        log.info("{} cost time: {} ms", joinPoint.getSignature().getName(), endTime - startTime);

        return proceed;
    }
}