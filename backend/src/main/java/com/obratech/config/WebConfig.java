
package com.obratech.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ActiveUserInterceptor activeUserInterceptor;
    private final SpaRoutingInterceptor spaRoutingInterceptor;

    public WebConfig(ActiveUserInterceptor activeUserInterceptor, SpaRoutingInterceptor spaRoutingInterceptor) {
        this.activeUserInterceptor = activeUserInterceptor;
        this.spaRoutingInterceptor = spaRoutingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        @SuppressWarnings("null")
        ActiveUserInterceptor safe = activeUserInterceptor;
        registry.addInterceptor(spaRoutingInterceptor)
            .addPathPatterns("/**")
            .order(0);
        registry.addInterceptor(safe)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/registro",
                        "/completar-registro-oauth2",
                        "/oauth2/**",
                        "/logout",
                        "/error",
                        "/uploads/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**");
    }
}
