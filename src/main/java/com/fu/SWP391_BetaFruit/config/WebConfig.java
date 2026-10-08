package com.fu.SWP391_BetaFruit.config;

import com.fu.SWP391_BetaFruit.interceptor.*;
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

    @Autowired
    private CustomerInterceptor customerInterceptor;

    @Autowired
    private ShipperInterceptor shipperInterceptor;

    @Autowired
    private StaffInterceptor staffInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");

        registry.addInterceptor(shopOwnerInterceptor)
                .addPathPatterns("/shop/**");

        registry.addInterceptor(customerInterceptor)
                .addPathPatterns("/customer/**");

        registry.addInterceptor(shipperInterceptor)
                .addPathPatterns("/shipper/**");

        registry.addInterceptor(staffInterceptor)
                .addPathPatterns("/staff/**");
    }
}
