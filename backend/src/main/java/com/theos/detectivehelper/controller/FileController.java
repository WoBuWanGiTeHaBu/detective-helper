package com.theos.detectivehelper.controller;

import com.theos.detectivehelper.common.Result;
import com.theos.detectivehelper.util.ImageUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    /**
     * 上传图片
     */
    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只支持图片文件");
            }

            // 验证文件大小 (10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.error("图片大小不能超过10MB");
            }

            // 读取图片
            BufferedImage image = ImageUtils.base64ToImage(java.util.Base64.getEncoder().encodeToString(file.getBytes()));

            // 缩放图片 (可选)
            BufferedImage resizedImage = ImageUtils.resizeImage(image, 800, 600);

            // 转换为Base64
            String base64Image = ImageUtils.imageToBase64(resizedImage, "png");

            Map<String, String> result = new HashMap<>();
            result.put("url", "data:image/png;base64," + base64Image);
            result.put("name", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件大小 (20MB)
            if (file.getSize() > 20 * 1024 * 1024) {
                return Result.error("文件大小不能超过20MB");
            }

            String base64Content = java.util.Base64.getEncoder().encodeToString(file.getBytes());

            Map<String, String> result = new HashMap<>();
            result.put("name", file.getOriginalFilename());
            result.put("size", String.valueOf(file.getSize()));
            result.put("type", file.getContentType());
            result.put("data", base64Content);

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }

}