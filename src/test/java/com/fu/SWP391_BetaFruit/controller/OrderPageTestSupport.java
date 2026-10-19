package com.fu.SWP391_BetaFruit.controller;

import com.fu.SWP391_BetaFruit.enums.RoleName;
import com.fu.SWP391_BetaFruit.interceptor.ShopOwnerInterceptor;
import com.fu.SWP391_BetaFruit.interceptor.ShipperInterceptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** Real Thymeleaf templates with isolated controller data, no application datasource. */
abstract class OrderPageTestSupport {
    protected MockMvc mvc;

    protected MockMvc pageMvc(RoleName role, Object controller) {
        var templates = new ClassLoaderTemplateResolver();
        templates.setPrefix("templates/"); templates.setSuffix(".html");
        templates.setTemplateMode("HTML"); templates.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine(); engine.setTemplateResolver(templates);
        var views = new ThymeleafViewResolver(); views.setTemplateEngine(engine);
        views.setCharacterEncoding("UTF-8");
        var session = new MockHttpSession(); session.setAttribute("LOGGED_IN_USER_ID", 7);
        session.setAttribute("LOGGED_IN_FULLNAME", "Nguyễn Minh Anh");
        session.setAttribute("LOGGED_IN_USERNAME", "owner");
        session.setAttribute("LOGGED_IN_ROLES", java.util.List.of(role));
        return standaloneSetup(controller).setViewResolvers(views)
                .defaultRequest(get("/").session(session))
                .addMappedInterceptors(new String[]{"/shop/**"}, new ShopOwnerInterceptor())
                .addMappedInterceptors(new String[]{"/shipper/**"}, new ShipperInterceptor()).build();
    }
}
