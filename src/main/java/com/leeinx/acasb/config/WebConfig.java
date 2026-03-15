package com.leeinx.acasb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.leeinx.acasb.jwt.AuthInterceptor;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Value("${app.dataset-storage-folder:./dataset-storage}")
    private String datasetStorageFolder;

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
            .addPathPatterns("/data/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String datasetStorageLocation = Paths.get(datasetStorageFolder)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        if (!datasetStorageLocation.endsWith("/")) {
            datasetStorageLocation = datasetStorageLocation + "/";
        }

        registry.addResourceHandler("/media/dataset/**")
                .addResourceLocations(datasetStorageLocation);
    }
}
