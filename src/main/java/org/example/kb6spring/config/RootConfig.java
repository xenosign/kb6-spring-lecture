package org.example.kb6spring.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.example.kb6spring"})
public class RootConfig {
    // 어플리케이션 전역에 필요한 Bean 은 RootConfig 에서 등록합니다!
    // @Service, @Repository, @Component 등
}
