package com.ailearning.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口（需登录）：支持图片与视频，统一保存到项目下的 projectfiles 目录
 */
@RestController
@RequestMapping("/upload")
public class FileUploadController {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "mov", "mkv", "avi");

    @Value("${file.upload-dir:projectfiles}")
    private String uploadDir;

    /** 单文件上传：返回可直接访问的 URL（/api/files/...） */
    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }

        String folder;
        if (IMAGE_EXT.contains(ext)) {
            folder = "images";
        } else if (VIDEO_EXT.contains(ext)) {
            folder = "videos";
        } else {
            throw new BizException("仅支持图片（jpg/png/gif/webp）或视频（mp4/webm/mov/mkv/avi）文件");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, folder).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败，请稍后重试");
        }
        return Result.ok(Map.of(
                "url", "/api/files/" + folder + "/" + filename,
                "name", original == null ? filename : original
        ));
    }
}