package io.github.ujjawalgupta29.filters;

import io.github.ujjawalgupta29.annotations.RateLimit;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RateLimitingFilter implements Filter {

    private ConcurrentMap<String, Integer> thresholdPerService = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Integer> thresholdPerUserPerService = new ConcurrentHashMap<>();

    private ConcurrentMap<String, Semaphore> servicePermits =  new ConcurrentHashMap<>();
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());
        ListableBeanFactory beanFactory = (ListableBeanFactory) ctx;
        String[] beanNames = beanFactory.getBeanNamesForAnnotation(RateLimit.class);
        for(String beanName : beanNames) {
            Class<?> beanClass = ((ListableBeanFactory) ctx).getType(beanName);
            if(beanClass != null) {
                RateLimit annotation = beanClass.getAnnotation(RateLimit.class);
                int definedThresholdPerService = annotation.thresholdPerService();
                if(definedThresholdPerService != -1) {
                    thresholdPerService.put(beanName, definedThresholdPerService);
                }

                int definedThresholdPerUserPerService = annotation.thresholdPerUserPerService();
                if(definedThresholdPerUserPerService != -1) {
                    thresholdPerUserPerService.put(beanName, definedThresholdPerUserPerService);
                }
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String serviceName = httpRequest.getServletPath().split("/")[1];

        if(serviceName == null || !thresholdPerService.containsKey(serviceName)) {
            filterChain.doFilter(request, response);
        }

        Semaphore semaphorePerService = null;
        boolean isServiceResourceRequired = false;
        try {

            semaphorePerService = getSemaphorePerService(serviceName);
            isServiceResourceRequired = tryAcquire(semaphorePerService, serviceName);

            filterChain.doFilter(request, response);
        } catch (RemoteException e) {
            httpResponse.setStatus(429);
        } finally {
            if(isServiceResourceRequired) {
                semaphorePerService.release();
            }
        }

    }

    private boolean tryAcquire(Semaphore semaphorePerService, String serviceName) throws RemoteException {
        boolean semaphoreAcquired = false;
        try {
            semaphoreAcquired = semaphorePerService.tryAcquire(1, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RemoteException("Failed to acquire permit for service " + serviceName);
        }

        if(!semaphoreAcquired) {
            throw new RemoteException("Threshold breached for service " + serviceName);
        }

        return semaphoreAcquired;
    }

    private synchronized Semaphore getSemaphorePerService(String serviceName) {
        Semaphore semaphorePerService = servicePermits.get(serviceName);
        if(semaphorePerService == null) {
            servicePermits.put(serviceName, new Semaphore(thresholdPerService.get(serviceName), true));
        }
        return servicePermits.get(serviceName);
    }

    @Override
    public void destroy() {

    }
}
