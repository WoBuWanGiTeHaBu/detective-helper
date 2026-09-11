package com.theos.detectivehelper.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图片工具类
 */
public class ImageUtils {

    /**
     * Base64字符串转图片
     */
    public static BufferedImage base64ToImage(String base64String) throws IOException {
        String[] parts = base64String.split(",");
        String imageData = parts.length > 1 ? parts[1] : parts[0];

        byte[] imageBytes = Base64.getDecoder().decode(imageData);
        ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
        return javax.imageio.ImageIO.read(bis);
    }

    /**
     * 图片转Base64字符串
     */
    public static String imageToBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, format, bos);
        byte[] imageBytes = bos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 缩放图片
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, width, height, null);
        g2d.dispose();
        return resizedImage;
    }

    /**
     * 裁剪图片
     */
    public static BufferedImage cropImage(BufferedImage originalImage, int x, int y, int width, int height) {
        return originalImage.getSubimage(x, y, width, height);
    }

    /**
     * 验证图片格式
     */
    public static boolean isValidImageFormat(String format) {
        return format.equalsIgnoreCase("jpg") || format.equalsIgnoreCase("jpeg") ||
               format.equalsIgnoreCase("png") || format.equalsIgnoreCase("gif") ||
               format.equalsIgnoreCase("bmp") || format.equalsIgnoreCase("webp");
    }

}