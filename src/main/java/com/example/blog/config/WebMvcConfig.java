package com.example.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final SiteProperties siteProperties;

    public WebMvcConfig(SiteProperties siteProperties) {
        this.siteProperties = siteProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(siteProperties.getUploadRoot().toUri().toString());
    }
}
