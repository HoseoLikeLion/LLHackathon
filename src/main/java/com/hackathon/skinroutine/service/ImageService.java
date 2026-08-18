package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.common.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 사진 검증 + 긴 변 1024px JPEG로 정규화.
 * 리사이즈 이유: 비전 API 비용·지연 절감 + Storage 용량 절약 (docs/05 스택 확정).
 * ⚠️ 아이폰 HEIC는 자바 표준 라이브러리가 못 읽는다 — 프론트 input에 accept="image/jpeg,image/png" 지정 필요.
 */
@Service
public class ImageService {

    private static final int MAX_DIMENSION = 1024;

    public byte[] toResizedJpeg(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw ApiException.badRequest("INVALID_IMAGE", "사진 파일이 비어 있어요.");
        }
        String contentType = photo.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw ApiException.badRequest("INVALID_IMAGE",
                    "이미지 파일만 올릴 수 있어요. (받은 형식: " + contentType + ")");
        }
        try (InputStream in = photo.getInputStream()) {
            BufferedImage source = ImageIO.read(in);
            if (source == null) {
                throw ApiException.badRequest("INVALID_IMAGE",
                        "이미지를 읽을 수 없어요. JPG 또는 PNG 형식으로 올려주세요.");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (source.getWidth() > MAX_DIMENSION || source.getHeight() > MAX_DIMENSION) {
                Thumbnails.of(source).size(MAX_DIMENSION, MAX_DIMENSION) // 비율 유지 축소
                        .outputFormat("jpg").outputQuality(0.85).toOutputStream(out);
            } else {
                Thumbnails.of(source).scale(1.0) // 작은 사진은 확대하지 않고 JPEG 재인코딩만
                        .outputFormat("jpg").outputQuality(0.85).toOutputStream(out);
            }
            return out.toByteArray();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("INVALID_IMAGE",
                    "이미지를 처리할 수 없어요. JPG 또는 PNG 형식으로 올려주세요.");
        }
    }
}
