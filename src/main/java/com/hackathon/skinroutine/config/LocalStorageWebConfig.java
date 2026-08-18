package com.hackathon.skinroutine.config;

import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * local 프로필 전용 — ./local-uploads 에 저장한 사진을
 * http://localhost:8080/local-photos/** 로 서빙해서 프론트 로컬 연동 시 이미지가 실제로 보이게 한다.
 */
@Configuration
@Profile("local")
public class LocalStorageWebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public LocalStorageWebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String dir = (props.storage() != null && props.storage().localDir() != null)
                ? props.storage().localDir() : "./local-uploads";
        String location = Paths.get(dir).toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/local-photos/**")
                .addResourceLocations(location);
    }
}
