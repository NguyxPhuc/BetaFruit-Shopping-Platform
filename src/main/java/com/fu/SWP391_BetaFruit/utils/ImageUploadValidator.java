package com.fu.SWP391_BetaFruit.utils;

import com.fu.SWP391_BetaFruit.exception.InvalidUploadImageException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;

@Component
public class ImageUploadValidator {

    private void validateFormat(byte[] content){
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new InvalidUploadImageException("Định dạng ảnh không được hỗ trợ. Hệ thống chỉ chấp nhận ảnh JPEG và PNG.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                if (!"JPEG".equalsIgnoreCase(format) && !"PNG".equalsIgnoreCase(format)) {
                    throw new InvalidUploadImageException("Chỉ chấp nhận tệp hình ảnh định dạng JPEG và PNG.");
                }
            } finally {
                reader.dispose();
            }
        } catch (InvalidUploadImageException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUploadImageException("Không thể nhận diện định dạng của tệp hình ảnh tải lên.");
        }
    }

    public void validate(byte[] content){
        if (content == null || content.length == 0) {
            throw new InvalidUploadImageException("Nội dung tệp hình ảnh không hợp lệ hoặc rỗng.");
        }
        validateFormat(content);
    }
}
