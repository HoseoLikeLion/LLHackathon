package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.config.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * local 프로필 전용 — Supabase 없이 ./local-uploads 폴더에 저장.
 * LocalStorageWebConfig가 /local-photos/** 로 서빙해서 프론트 로컬 연동에서도 이미지가 보인다.
 */
@Service
@Profile("local")
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final AppProperties props;

    public LocalStorageService(AppProperties props) {
        this.props = props;
    }

    @Override
    public String uploadJpeg(byte[] jpegBytes, UUID userId, LocalDate date) {
        try {
            String dir = (props.storage() != null && props.storage().localDir() != null)
                    ? props.storage().localDir() : "./local-uploads";
            Path folder = Paths.get(dir, "records", userId.toString());
            Files.createDirectories(folder);
            String filename = date + "-" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            Files.write(folder.resolve(filename), jpegBytes);
            // 로컬 개발 전용이라 포트 8080 고정
            return "http://localhost:8080/local-photos/records/" + userId + "/" + filename;
        } catch (Exception e) {
            log.error("로컬 사진 저장 실패 — photoUrl 없이 진행: {}", e.toString());
            return null;
        }
    }
}
