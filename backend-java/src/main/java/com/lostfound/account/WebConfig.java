package com.lostfound.account;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AgreementInterceptor agreementInterceptor;

    public WebConfig(AgreementInterceptor agreementInterceptor) {
        this.agreementInterceptor = agreementInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agreementInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String mediaDir = Paths.get("./data/media").toAbsolutePath().normalize().toUri().toString();
        if (!mediaDir.endsWith("/")) {
            mediaDir = mediaDir + "/";
        }
        registry.addResourceHandler("/media/**").addResourceLocations(mediaDir);
    }
}
