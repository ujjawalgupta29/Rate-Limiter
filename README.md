**Rate Limiter**

Implementation of a Rate Limiter using semaphores to block number of active api calls at a service level if it reaches beyond it's threshold.

We just need to add below dependency, register its bean in filter chain and use annotation to use Rate Limiter.

```
<dependency>
  <groupId>io.github.ujjawalgupta29</groupId>
  <artifactId>RateLimiter_Semaphores</artifactId>
  <version>2.0.0.RC1</version>
</dependency>
```

```
@Bean
public FilterRegistrationBean<RateLimitingFilter> rateLimiterFilter() {
    FilterRegistrationBean<RateLimitingFilter> filterBean = new FilterRegistrationBean<>();
    filterBean.setFilter(new RateLimitingFilter());
    filterBean.addUrlPatterns("/*");
    filterBean.setOrder(1);;
    return filterBean;
}
```

Sample Code:
```
@RestController
@RequestMapping("/helloApis")
@RateLimit(
        thresholdPerService = 5
)
public class HelloApis {
    @GetMapping(path = "/hello-world")
    @ResponseBody
    public String helloWorld() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "Hello World";
    }
}
```
