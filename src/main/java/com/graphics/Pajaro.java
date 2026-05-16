package com.graphics;

public class Pajaro {
    public float x, y;
    public float velY;
    public boolean estaVivo;
    public int puntaje;
    public float r, g, b;
    public float tiempoAleteo;
    public float tiempoMuerto = 0f;

    public Pajaro(float x, float y) {
        this.x = x;
        this.y = y;
        this.velY = 0f;
        this.estaVivo = true;
        this.puntaje = 0;
        this.r = 0.98f;
        this.g = 0.85f;
        this.b = 0.20f;
        this.tiempoAleteo = 0f;
        this.tiempoMuerto = 0f;
    }

    public void actualizarAnimacion(float dt) {
        float incremento = dt * 6.0f; // velocidad base
        if (velY > 0) { // impulso vertical positivo (salto)
            incremento *= 2.0f; // duplica temporalmente la velocidad para simular esfuerzo sincronizado
        }
        tiempoAleteo += incremento;
        if (tiempoAleteo > Math.PI * 2) {
            tiempoAleteo -= (float) (Math.PI * 2);
        }
    }

    public void saltar(float impulso) {
        velY = impulso;
    }

    public void dibujar(Renderer renderizador) {
        float angulo = velY * 0.6f;
        if (angulo > 0.8f)
            angulo = 0.8f;
        if (angulo < -0.8f)
            angulo = -0.8f;

        float alpha = 1.0f;
        if (!estaVivo) {
            angulo = -1.5f; // Cae en picada
            // Parpadeo rapido si acaba de morir
            if (tiempoMuerto < 1.0f) {
                alpha = (float) (Math.abs(Math.sin(tiempoMuerto * 30.0f))) * 0.5f + 0.5f;
            }
        }

        renderizador.dibujarCirculo(x, y, angulo, 0.0f, 0.0f, 0.12f, 0.12f, r, g, b, alpha);
        renderizador.dibujarCirculo(x, y, angulo, 0.03f, 0.02f, 0.04f, 0.04f, 1.0f, 1.0f, 1.0f, alpha);
        renderizador.dibujarCirculo(x, y, angulo, 0.038f, 0.02f, 0.015f, 0.015f, 0.0f, 0.0f, 0.0f, alpha);
        renderizador.dibujarTriangulo(x, y, angulo, 0.07f, -0.01f, 0.06f, 0.04f, 1.0f, 0.5f, 0.0f, alpha);
        renderizador.dibujarTriangulo(x, y, angulo, -0.07f, 0.0f, -0.05f, 0.05f, 0.9f, 0.3f, 0.0f, alpha);

        float aleteo = (float) Math.sin(tiempoAleteo);
        if (!estaVivo)
            aleteo = 0; // No aletea muerto
        float alaAlto = 0.08f * (0.3f + 0.7f * aleteo);
        renderizador.dibujarTriangulo(x, y, angulo, -0.01f, 0.0f, -0.06f, alaAlto, 0.9f, 0.7f, 0.1f, alpha);
    }
}
