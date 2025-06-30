package org.example.kb6spring.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kb6spring.security.filter.JwtAuthenticationFilter;
import org.example.kb6spring.security.filter.JwtLoginFilter;
import org.example.kb6spring.security.jwt.JwtTokenProvider;
import org.example.kb6spring.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .httpBasic().disable()
            .csrf().disable()
            .formLogin().disable();

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.authorizeRequests()
                .antMatchers("/").permitAll()
                .antMatchers("/user/**").permitAll()
                .antMatchers("/oauth/**").permitAll() // Oauth 접근은 모두 허용
                .antMatchers("/auth/admin").hasRole("ADMIN")
                .antMatchers("/auth/member").hasAnyRole("ADMIN", "MEMBER")
                .antMatchers("/auth/**").authenticated()
                .antMatchers("/index-test/**").permitAll()
                .antMatchers("/index-test2/**").permitAll()
                .antMatchers("/**").authenticated();

        http.addFilterBefore(new JwtLoginFilter(authenticationManager(), jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public CharacterEncodingFilter encodingFilter() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return encodingFilter;
    }

//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .csrf().disable()
//                .sessionManagement()
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 💡 세션 사용 안 함
//                .and()
//                .authorizeRequests()
//                .antMatchers("/").permitAll()
//                .antMatchers("/user/**").permitAll()
//                .antMatchers("/admin/**").hasRole("ADMIN")
//                .antMatchers("/member/**").hasAnyRole("ADMIN", "MEMBER")
//                .antMatchers("/**").authenticated()
//                .and()
//                .addFilterBefore(encodingFilter(), CsrfFilter.class)
//                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
//                .addFilter(loginFilter);
//    }

//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests()
//                .antMatchers("/").permitAll()
//                .antMatchers("/user/**").permitAll()
//                .antMatchers("/admin/**").access("hasRole('ROLE_ADMIN')")
//                .antMatchers("/member/**").access("hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')")
//                .antMatchers("/**").authenticated();
//
//        http.addFilterBefore(encodingFilter(), CsrfFilter.class);
//
//        http.formLogin()
//                .loginPage("/user/login")
//                .loginProcessingUrl("/user/login")
//                .defaultSuccessUrl("/user/login-success")
//                .failureUrl("/user/login-failure");
//
//        http.logout()
//                .logoutUrl("/user/logout")
//                .invalidateHttpSession(true)
//                .deleteCookies("remember-me", "JSESSIONID")
//                .logoutSuccessUrl("/user/login")
//                .permitAll();
//    }
}
