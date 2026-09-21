package com.fu.SWP391_BetaFruit.config;

import com.fu.SWP391_BetaFruit.interceptor.AdminInterceptor;
import com.fu.SWP391_BetaFruit.interceptor.ShopOwnerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AdminInterceptor adminInterceptor;

    @Autowired
    private ShopOwnerInterceptor shopOwnerInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");

        registry.addInterceptor(shopOwnerInterceptor)
                .addPathPatterns("/shop/**")
                .excludePathPatterns("/shop/public/**");
    }
}
