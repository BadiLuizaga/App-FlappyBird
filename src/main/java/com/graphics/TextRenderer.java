package com.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class TextRenderer {
    private int width = 512;
    private int height = 128;
    private int id;
    private BufferedImage image;
    private Graphics2D g2d;

    public TextRenderer(int w, int h) {
        this.width = w;
        this.height = h;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        id = GL11.glGenTextures();
    }

    public void renderText(String text, int size, Color color, boolean center, float alpha) {
        // Limpiar
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, width, height);
        g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));

        g2d.setFont(new Font("Arial", Font.BOLD, size));
        g2d.setColor(color);
        FontMetrics fm = g2d.getFontMetrics();
        int x = center ? (width - fm.stringWidth(text)) / 2 : 10;
        int y = ((height - fm.getHeight()) / 2) + fm.getAscent();

        // Sombras simples para legibilidad
        g2d.setColor(new Color(0, 0, 0, (int)(255*alpha)));
        g2d.drawString(text, x + 2, y + 2);
        
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255*alpha)));
        g2d.drawString(text, x, y);

        uploadTexture();
    }

    private void uploadTexture() {
        int[] pixels = new int[width * height * 4];
        pixels = image.getRGB(0, 0, width, height, null, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    }

    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }

    public void cleanup() {
        g2d.dispose();
        GL11.glDeleteTextures(id);
    }
}
