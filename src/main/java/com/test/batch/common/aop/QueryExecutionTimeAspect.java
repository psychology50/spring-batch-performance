package com.test.batch.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class QueryExecutionTimeAspect {

    @Around("execution(* com.test.batch.repository.*.*(..))")
    public Object measureQueryTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = joinPoint.proceed();
        stopWatch.stop();
        log.info("{} : Query executed in {} ms", joinPoint.getClass().getSimpleName(), stopWatch.getTotalTimeMillis());
        return result;
    }
}
