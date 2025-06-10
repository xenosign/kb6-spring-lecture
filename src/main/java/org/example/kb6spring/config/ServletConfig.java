package org.example.kb6spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@EnableWebMvc
//@ComponentScan(basePackages = {"org.example.kb6spring.controller"})
public class ServletConfig implements WebMvcConfigurer {
    // 웹 관련 Bean 은 ServletConfig 에서 등록 합니다
    // @Controller, @RestController 등
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    // jsp view resolver 설정
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry){
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/");
        bean.setSuffix(".jsp");
        registry.viewResolver(bean);
    }
}