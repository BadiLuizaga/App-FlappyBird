package com.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class Texture {
    private int id;
    private int width;
    private int height;

    public Texture(String resourcePath) {
        try (InputStream is = Texture.class.getResourceAsStream("/" + resourcePath)) {
            if (is == null) {
                System.err.println("No se encontro textura en: " + resourcePath);
                return;
            }
            BufferedImage image = ImageIO.read(is);
            width = image.getWidth();
            height = image.getHeight();

            int[] pixels = new int[width * height * 4];
            pixels = image.getRGB(0, 0, width, height, null, 0, width);

            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
                    buffer.put((byte) ((pixel >> 8) & 0xFF)); // G
                    buffer.put((byte) (pixel & 0xFF)); // B
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
                }
            }
            buffer.flip();

            id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE, buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }

    public void cleanup() {
        GL11.glDeleteTextures(id);
    }
}
