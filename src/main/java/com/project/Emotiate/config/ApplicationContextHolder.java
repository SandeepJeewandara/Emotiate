package com.project.Emotiate.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    // Static accessor used by JADE agents to retrieve Spring-managed beans
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}