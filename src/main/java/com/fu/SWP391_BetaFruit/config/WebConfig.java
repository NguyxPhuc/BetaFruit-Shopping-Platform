package com.fu.SWP391_BetaFruit.config;

import com.fu.SWP391_BetaFruit.interceptor.AdminInterceptor;
import com.fu.SWP391_BetaFruit.interceptor.CustomerInterceptor;
import com.fu.SWP391_BetaFruit.interceptor.ShipperInterceptor;
import com.fu.SWP391_BetaFruit.interceptor.ShopOwnerInterceptor;
import com.fu.SWP391_BetaFruit.interceptor.StaffInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AdminInterceptor adminInterceptor;

    @Autowired
    private StaffInterceptor staffInterceptor;

    @Autowired
    private ShopOwnerInterceptor shopOwnerInterceptor;

    @Autowired
    private CustomerInterceptor customerInterceptor;

    @Autowired
    private ShipperInterceptor shipperInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**");

        registry.addInterceptor(staffInterceptor)
                .addPathPatterns("/staff/**");

        registry.addInterceptor(shopOwnerInterceptor)
                .addPathPatterns("/shop/**")
                .excludePathPatterns("/shop/public/**");

        registry.addInterceptor(customerInterceptor)
                .addPathPatterns("/customer/**");

        registry.addInterceptor(shipperInterceptor)
                .addPathPatterns("/shipper/**");
    }
}
