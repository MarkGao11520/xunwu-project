package com.gwf.xunwu.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

/**
 * ApplicationContextAware 可以提供上下文
 */
@Configuration
public class WebMvcConfig
        extends WebMvcConfigurerAdapter
        implements ApplicationContextAware{
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/404").setViewName("/404");
        registry.addViewController("/500").setViewName("/500");
        registry.addViewController("/403").setViewName("/403");
        registry.addViewController("/logout/page").setViewName("/logout");
        registry.addViewController("/").setViewName("/index");
        registry.addViewController("/index").setViewName("/index");
        registry.addViewController("/admin/center").setViewName("/admin/center");
        registry.addViewController("/admin/welcome").setViewName("/admin/welcome");
        registry.addViewController("/admin/add/house").setViewName("admin/house-add");
        registry.addViewController("/admin/house/list").setViewName("admin/house-list");
        registry.addViewController("/admin/login").setViewName("/admin/login");
        registry.addViewController("/user/login").setViewName("/user/login");
        registry.addViewController("/user/center").setViewName("/user/center");
    }

    /**
     * 静态资源加载配置
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }

    /**
     * 模板资源解析器
     * @return
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.thymeleaf")
    public SpringResourceTemplateResolver templateResolver(){
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        // 设置spring上下文
        templateResolver.setApplicationContext(applicationContext);
        // 设置语法格式
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    /**
     * Thymeleaf 标准方言解析器
     */
    @Bean
    public SpringTemplateEngine templateEngine(){
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        // 支持SpringEL表达式
        templateEngine.setEnableSpringELCompiler(true);

        // 支持SpringSecurity方言
        SpringSecurityDialect securityDialect = new SpringSecurityDialect();
        templateEngine.addDialect(securityDialect);
        return templateEngine;
    }

    /**
     * 视图解析器
     */
    @Bean
    public ThymeleafViewResolver viewResolver(){
        ThymeleafViewResolver viewResolver =  new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        return viewResolver;
    }

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }
}
