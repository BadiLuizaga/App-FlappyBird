package com.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

public class Renderer {

    private int vaoQuad, vboQuad;
    private int vaoCirculo, vboCirculo;
    private int vaoTriangulo, vboTriangulo;
    private int numVerticesCirculo = 32;

    private int uColorLocation;
    private int uModelMatrixLocation;
    private int uUseTextureLocation;
    private FloatBuffer matrixBuffer;

    public Renderer(int programa) {
        uColorLocation = GL20.glGetUniformLocation(programa, "uColor");
        uModelMatrixLocation = GL20.glGetUniformLocation(programa, "uModelMatrix");
        uUseTextureLocation = GL20.glGetUniformLocation(programa, "uUseTexture");
        matrixBuffer = BufferUtils.createFloatBuffer(16);

        crearQuad();
        crearCirculo();
        crearTriangulo();
    }

    private void crearQuad() {
        float[] vertices = {
                -0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
                0.5f, -0.5f, 0.0f, 1.0f, 1.0f,
                0.5f, 0.5f, 0.0f, 1.0f, 0.0f,
                -0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
                0.5f, 0.5f, 0.0f, 1.0f, 0.0f,
                -0.5f, 0.5f, 0.0f, 0.0f, 0.0f
        };
        vaoQuad = GL30.glGenVertexArrays();
        vboQuad = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoQuad);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboQuad);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
        GL30.glBindVertexArray(0);
    }

    private void crearCirculo() {
        float[] vertices = new float[(numVerticesCirculo + 2) * 3];
        vertices[0] = 0.0f;
        vertices[1] = 0.0f;
        vertices[2] = 0.0f;
        for (int i = 0; i <= numVerticesCirculo; i++) {
            float angle = (float) (i * 2.0 * Math.PI / numVerticesCirculo);
            vertices[3 + i * 3] = (float) (Math.cos(angle) * 0.5f);
            vertices[4 + i * 3] = (float) (Math.sin(angle) * 0.5f);
            vertices[5 + i * 3] = 0.0f;
        }
        vaoCirculo = GL30.glGenVertexArrays();
        vboCirculo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoCirculo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboCirculo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
    }

    private void crearTriangulo() {
        float[] vertices = {
                -0.5f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, 0.0f, 0.0f
        };
        vaoTriangulo = GL30.glGenVertexArrays();
        vboTriangulo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vaoTriangulo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTriangulo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
    }

    private void aplicarMatrizYColor(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b, float a) {
        float c = (float) Math.cos(baseRot);
        float s = (float) Math.sin(baseRot);

        float m00 = scaleX * c;
        float m10 = -scaleY * s;
        float m20 = 0;
        float m30 = baseX + localX * c - localY * s;
        float m01 = scaleX * s;
        float m11 = scaleY * c;
        float m21 = 0;
        float m31 = baseY + localX * s + localY * c;
        float m02 = 0;
        float m12 = 0;
        float m22 = 1;
        float m32 = 0;
        float m03 = 0;
        float m13 = 0;
        float m23 = 0;
        float m33 = 1;

        matrixBuffer.clear();
        matrixBuffer.put(m00).put(m01).put(m02).put(m03);
        matrixBuffer.put(m10).put(m11).put(m12).put(m13);
        matrixBuffer.put(m20).put(m21).put(m22).put(m23);
        matrixBuffer.put(m30).put(m31).put(m32).put(m33);
        matrixBuffer.flip();

        GL20.glUniformMatrix4fv(uModelMatrixLocation, false, matrixBuffer);
        GL20.glUniform4f(uColorLocation, r, g, b, a); // changed to 4f for alpha
    }

    public void dibujarRect(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b, float a) {
        aplicarMatrizYColor(baseX, baseY, baseRot, localX, localY, scaleX, scaleY, r, g, b, a);
        GL20.glUniform1i(uUseTextureLocation, 0);
        GL30.glBindVertexArray(vaoQuad);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    // Compat method
    public void dibujarRect(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b) {
        dibujarRect(baseX, baseY, baseRot, localX, localY, scaleX, scaleY, r, g, b, 1.0f);
    }

    public void dibujarCirculo(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b, float a) {
        aplicarMatrizYColor(baseX, baseY, baseRot, localX, localY, scaleX, scaleY, r, g, b, a);
        GL20.glUniform1i(uUseTextureLocation, 0);
        GL30.glBindVertexArray(vaoCirculo);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, numVerticesCirculo + 2);
    }

    public void dibujarCirculo(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b) {
        dibujarCirculo(baseX, baseY, baseRot, localX, localY, scaleX, scaleY, r, g, b, 1.0f);
    }

    public void dibujarTriangulo(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b, float a) {
        aplicarMatrizYColor(baseX, baseY, baseRot, localX, localY, scaleX, scaleY, r, g, b, a);
        GL20.glUniform1i(uUseTextureLocation, 0);
        GL30.glBindVertexArray(vaoTriangulo);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    public void dibujarTriangulo(float baseX, float baseY, float baseRot, float localX, float localY, float scaleX,
            float scaleY, float r, float g, float b) {
        dibujarTriangulo(baseX, baseY, baseRot, localX, localY, scaleX, scaleY, r, g, b, 1.0f);
    }

    public void dibujarTextura(Texture tex, float x, float y, float w, float h, float a) {
        aplicarMatrizYColor(x, y, 0, 0, 0, w, h, 1, 1, 1, a);
        GL20.glUniform1i(uUseTextureLocation, 1);
        tex.bind();
        GL30.glBindVertexArray(vaoQuad);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    public void dibujarTextRenderer(TextRenderer tr, float x, float y, float w, float h, float a) {
        aplicarMatrizYColor(x, y, 0, 0, 0, w, h, 1, 1, 1, a);
        GL20.glUniform1i(uUseTextureLocation, 1);
        tr.bind();
        GL30.glBindVertexArray(vaoQuad);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vaoQuad);
        GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoCirculo);
        GL15.glDeleteBuffers(vboCirculo);
        GL30.glDeleteVertexArrays(vaoTriangulo);
        GL15.glDeleteBuffers(vboTriangulo);
    }
}
