package com.company.finance_api.profile.avatar;

import com.company.finance_api.config.ProfileAvatarProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class ProfileAvatarImageProcessor {

    private final ProfileAvatarProperties properties;

    public ProfileAvatarImageProcessor(ProfileAvatarProperties properties) {
        this.properties = properties;
    }

    /**
     * Decodes an uploaded bitmap, scales to a bounded square, and encodes as baseline JPEG.
     */
    public byte[] toOptimizedJpeg(byte[] raw) {
        if (raw.length > properties.getMaxUploadBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image upload is too large");
        }
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image", e);
        }
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported or corrupt image format");
        }
        int w = source.getWidth();
        int h = source.getHeight();
        if (w <= 0 || h <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image dimensions");
        }
        int max = properties.getMaxEdgePixels();
        BufferedImage rgbForEncode;
        if (Math.max(w, h) > max) {
            double scale = (double) max / (double) Math.max(w, h);
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            rgbForEncode = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbForEncode.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, nw, nh);
                g.drawImage(source, 0, 0, nw, nh, null);
            } finally {
                g.dispose();
            }
        } else if (source.getType() != BufferedImage.TYPE_INT_RGB) {
            rgbForEncode = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbForEncode.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);
                g.drawImage(source, 0, 0, null);
            } finally {
                g.dispose();
            }
        } else {
            rgbForEncode = source;
        }
        return writeJpeg(rgbForEncode, properties.getJpegQuality());
    }

    private byte[] writeJpeg(BufferedImage image, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.min(1f, Math.max(0.05f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not encode avatar image", e);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }
}
