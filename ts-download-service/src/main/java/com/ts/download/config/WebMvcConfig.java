package com.ts.download.config;

import com.ts.download.crypto.CryptoFilter;
import com.ts.download.crypto.RsaUtil;
import com.ts.download.interceptor.AuthInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/crypto/publicKey"
                );
    }

    @Bean
    public FilterRegistrationBean<CryptoFilter> cryptoFilterRegistration(RsaUtil rsaUtil) {
        CryptoFilter filter = new CryptoFilter();
        filter.setRsaUtil(rsaUtil);
        FilterRegistrationBean<CryptoFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
