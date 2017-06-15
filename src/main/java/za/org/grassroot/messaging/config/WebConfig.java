package za.org.grassroot.messaging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import za.org.grassroot.messaging.service.jwt.JwtAuthInterceptor;

/**
 * Created by luke on 2017/05/22.
 */
@Configuration
@ControllerAdvice
public class WebConfig extends WebMvcConfigurerAdapter {
    
    // since this service will only ever be called by other services, which will
    // have auth tokens, so we intercept everything
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/jwt/public/**");
    }

    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor() {
        return new JwtAuthInterceptor();
    }

}
