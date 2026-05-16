package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import java.awt.Color;

/**
 * AppFlappyBird:
 * Mini-juego estilo Flappy Bird con OpenGL 2D.
 * Soporte para 2 jugadores simultáneos y escalado de dificultad progresivo.
 */
public class AppFlappyBird {

    private static final int ANCHO = 900;
    private static final int ALTO = 700;

    private static final float BIRD_X = -0.4f;
    private static final float BIRD_ANCHO = 0.10f;
    private static final float BIRD_ALTO = 0.10f;
    private static final float GRAVEDAD = -1.9f;
    private static final float IMPULSO_SALTO = 0.85f;
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    private static final float TUBERIA_ANCHO = 0.18f;
    private static final float GAP_ALTO = 0.48f;
    private static final float GAP_MIN_CENTRO = -0.45f;
    private static final float GAP_MAX_CENTRO = 0.45f;

    private int nivelActual = 1;
    private float velocidadTuberias = 0.62f;
    private float tiempoEntreTuberias = 1.8f;
    private static final float MAX_VELOCIDAD = 1.2f;
    private static final float MIN_TIEMPO_APARICION = 1.0f;

    private long window;
    private int programa;

    private Renderer renderizador;
    private Texture texFondo;
    private TextRenderer trHud1;
    private TextRenderer trHud2;
    private TextRenderer trGameOver;
    private TextRenderer trTitle;

    private SoundPlayer sonidoSalto;
    private SoundPlayer sonidoPunto;
    private SoundPlayer sonidoGameOver;

    private Pajaro j1;
    private Pajaro j2;

    private float timerSpawn;
    private boolean started;
    private boolean gameOver;
    private boolean prevSpace;
    private boolean prevW;
    private boolean prevR;

    private final List<Tuberia> tuberias = new ArrayList<>();
    private final Random random = new Random();

    private static class Tuberia {
        float x;
        float gapCentroY;
        boolean puntuada;

        Tuberia(float x, float gapCentroY) {
            this.x = x;
            this.gapCentroY = gapCentroY;
        }
    }

    public void run() {
        init();
        resetGame();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird OpenGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        // Habilitar blending para texturas y transparencias
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        crearShaders();
        renderizador = new Renderer(programa);

        // Carga de Textura y Renderizadores de Texto
        texFondo = new Texture("background.png");
        trHud1 = new TextRenderer(256, 128);
        trHud2 = new TextRenderer(256, 128);
        trGameOver = new TextRenderer(512, 256);
        trTitle = new TextRenderer(512, 128);

        sonidoSalto = new SoundPlayer("/sounds/sfx_wing.wav");
        sonidoPunto = new SoundPlayer("/sounds/sfx_point.wav");
        sonidoGameOver = new SoundPlayer("/sounds/sfx_die.wav");

        trTitle.renderText("Flappy Bird", 48, Color.WHITE, true, 1.0f);

        j1 = new Pajaro(BIRD_X, 0.0f);
        j1.r = 1.0f;
        j1.g = 1.0f;
        j1.b = 0.0f;

        j2 = new Pajaro(BIRD_X, 0.0f);
        j2.r = 1.0f;
        j2.g = 0.0f;
        j2.b = 0.0f;
    }

    private void crearShaders() {
        String vertexSrc = """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec2 aTexCoord;
                uniform mat4 uModelMatrix;
                out vec2 vTexCoord;
                void main() {
                    gl_Position = uModelMatrix * vec4(aPos, 1.0);
                    vTexCoord = aTexCoord;
                }
                """;

        String fragmentSrc = """
                #version 330 core
                uniform vec4 uColor;
                uniform bool uUseTexture;
                uniform sampler2D uTexture;
                in vec2 vTexCoord;
                out vec4 fragColor;
                void main() {
                    if (uUseTexture) {
                        vec4 texColor = texture(uTexture, vTexCoord);
                        fragColor = texColor * uColor;
                    } else {
                        fragColor = uColor;
                    }
                }
                """;

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + GL20.glGetProgramInfoLog(programa));
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    private void actualizarTextos() {
        trHud1.renderText("J1: " + j1.puntaje, 36, Color.YELLOW, true, 1.0f);
        trHud2.renderText("J2: " + j2.puntaje, 36, Color.RED, true, 1.0f);
    }

    private void resetGame() {
        if (j1 != null) {
            j1.y = 0.0f;
            j1.velY = 0.0f;
            j1.puntaje = 0;
            j1.estaVivo = true;
            j1.tiempoAleteo = 0f;
            j1.tiempoMuerto = 0f;
        }
        if (j2 != null) {
            j2.y = 0.0f;
            j2.velY = 0.0f;
            j2.puntaje = 0;
            j2.estaVivo = true;
            j2.tiempoAleteo = 0f;
            j2.tiempoMuerto = 0f;
        }
        timerSpawn = 0.0f;
        started = false;
        gameOver = false;
        tuberias.clear();

        nivelActual = 1;
        velocidadTuberias = 0.62f;
        tiempoEntreTuberias = 1.8f;

        actualizarTextos();
        actualizarTitulo();
    }

    private void saltarPajaro(Pajaro p) {
        p.saltar(IMPULSO_SALTO);
        if (sonidoSalto != null)
            sonidoSalto.play();
    }

    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceAhora && !prevSpace) {
            if (gameOver) {
                resetGame();
                started = true;
                if (j1.estaVivo)
                    saltarPajaro(j1);
                if (j2.estaVivo)
                    saltarPajaro(j2);
            } else if (!started) {
                started = true;
                if (j1.estaVivo)
                    saltarPajaro(j1);
                if (j2.estaVivo)
                    saltarPajaro(j2);
            } else {
                if (j1.estaVivo)
                    saltarPajaro(j1);
            }
        }
        prevSpace = spaceAhora;

        boolean wAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        if (wAhora && !prevW) {
            if (gameOver) {
                resetGame();
                started = true;
                if (j1.estaVivo)
                    saltarPajaro(j1);
                if (j2.estaVivo)
                    saltarPajaro(j2);
            } else if (!started) {
                started = true;
                if (j1.estaVivo)
                    saltarPajaro(j1);
                if (j2.estaVivo)
                    saltarPajaro(j2);
            } else {
                if (j2.estaVivo)
                    saltarPajaro(j2);
            }
        }
        prevW = wAhora;

        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rAhora && !prevR && gameOver) {
            resetGame();
        }
        prevR = rAhora;
    }

    private void actualizarDificultad() {
        int puntajeTotal = (j1 != null ? j1.puntaje : 0) + (j2 != null ? j2.puntaje : 0);

        if (puntajeTotal >= 30) {
            nivelActual = 5;
            velocidadTuberias = 1.15f;
            tiempoEntreTuberias = 1.0f;
        } else if (puntajeTotal >= 20) {
            nivelActual = 4;
            velocidadTuberias = 1.0f;
            tiempoEntreTuberias = 1.15f;
        } else if (puntajeTotal >= 12) {
            nivelActual = 3;
            velocidadTuberias = 0.85f;
            tiempoEntreTuberias = 1.3f;
        } else if (puntajeTotal >= 5) {
            nivelActual = 2;
            velocidadTuberias = 0.72f;
            tiempoEntreTuberias = 1.5f;
        } else {
            nivelActual = 1;
            velocidadTuberias = 0.62f;
            tiempoEntreTuberias = 1.8f;
        }

        if (velocidadTuberias > MAX_VELOCIDAD)
            velocidadTuberias = MAX_VELOCIDAD;
        if (tiempoEntreTuberias < MIN_TIEMPO_APARICION)
            tiempoEntreTuberias = MIN_TIEMPO_APARICION;
    }

    private void morir(Pajaro p) {
        if (p.estaVivo) {
            p.estaVivo = false;
        }
    }

    private void actualizar(float dt) {
        j1.actualizarAnimacion(dt);
        j2.actualizarAnimacion(dt);

        if (!started || gameOver) {
            return;
        }

        if (j1.estaVivo || (!j1.estaVivo && j1.y > -1.1f)) {
            j1.velY += GRAVEDAD * dt;
            if (j1.velY < VELOCIDAD_MAX_CAIDA)
                j1.velY = VELOCIDAD_MAX_CAIDA;
            j1.y += j1.velY * dt;
            if (!j1.estaVivo)
                j1.tiempoMuerto += dt;

            float birdTop = j1.y + (BIRD_ALTO * 0.5f);
            float birdBottom = j1.y - (BIRD_ALTO * 0.5f);
            if (birdTop >= 1.0f || birdBottom <= -1.0f) {
                morir(j1);
            }
        }

        if (j2.estaVivo || (!j2.estaVivo && j2.y > -1.1f)) {
            j2.velY += GRAVEDAD * dt;
            if (j2.velY < VELOCIDAD_MAX_CAIDA)
                j2.velY = VELOCIDAD_MAX_CAIDA;
            j2.y += j2.velY * dt;
            if (!j2.estaVivo)
                j2.tiempoMuerto += dt;

            float birdTop = j2.y + (BIRD_ALTO * 0.5f);
            float birdBottom = j2.y - (BIRD_ALTO * 0.5f);
            if (birdTop >= 1.0f || birdBottom <= -1.0f) {
                morir(j2);
            }
        }

        if (!j1.estaVivo && !j2.estaVivo) {
            if (!gameOver) {
                if (sonidoGameOver != null)
                    sonidoGameOver.play();
                trGameOver.renderText("FINAL  J1: " + j1.puntaje + "   J2: " + j2.puntaje, 36, Color.WHITE, true, 1.0f);
            }
            gameOver = true;
            actualizarTitulo();
            return;
        }

        timerSpawn += dt;
        if (timerSpawn >= tiempoEntreTuberias) {
            timerSpawn = 0.0f;
            spawnTuberia();
        }

        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            t.x -= velocidadTuberias * dt;

            if (t.x + (TUBERIA_ANCHO * 0.5f) < BIRD_X && !t.puntuada) {
                t.puntuada = true;
                if (sonidoPunto != null)
                    sonidoPunto.play();
                if (j1.estaVivo)
                    j1.puntaje++;
                if (j2.estaVivo)
                    j2.puntaje++;

                actualizarTextos();
                actualizarDificultad();
                actualizarTitulo();
            }

            if (j1.estaVivo && colisionaConTuberia(t, j1))
                morir(j1);
            if (j2.estaVivo && colisionaConTuberia(t, j2))
                morir(j2);

            if (!j1.estaVivo && !j2.estaVivo) {
                if (!gameOver) {
                    if (sonidoGameOver != null)
                        sonidoGameOver.play();
                    trGameOver.renderText("FINAL  J1: " + j1.puntaje + "   J2: " + j2.puntaje, 36, Color.WHITE, true,
                            1.0f);
                }
                gameOver = true;
                actualizarTitulo();
                return;
            }

            if (t.x + (TUBERIA_ANCHO * 0.5f) < -1.3f) {
                it.remove();
            }
        }
    }

    private void spawnTuberia() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, gapCentro));
    }

    private boolean colisionaConTuberia(Tuberia t, Pajaro p) {
        float birdLeft = p.x - (BIRD_ANCHO * 0.5f);
        float birdRight = p.x + (BIRD_ANCHO * 0.5f);
        float birdBottom = p.y - (BIRD_ALTO * 0.5f);
        float birdTop = p.y + (BIRD_ALTO * 0.5f);

        float pipeLeft = t.x - (TUBERIA_ANCHO * 0.5f);
        float pipeRight = t.x + (TUBERIA_ANCHO * 0.5f);
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) {
            return false;
        }

        float gapTop = t.gapCentroY + (GAP_ALTO * 0.5f);
        float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    private void dibujarTuberia3D(float x, float centerY, float alto, boolean isTop) {
        float w = TUBERIA_ANCHO;
        float capH = 0.08f;
        float capW = TUBERIA_ANCHO + 0.04f; // Pestaña más ancha

        float edgeX = x - w / 2;
        float rightX = x + w / 2;

        // Cuerpo: gradiente simulado dibujando 3 tiras (oscuro, claro, oscuro)
        renderizador.dibujarRect(x - w * 0.25f, centerY, 0f, 0f, 0f, w * 0.5f, alto, 0.2f, 0.6f, 0.2f, 1.0f); // Izquierda
                                                                                                              // oscura
        renderizador.dibujarRect(x, centerY, 0f, 0f, 0f, w * 0.4f, alto, 0.4f, 0.8f, 0.3f, 1.0f); // Centro claro
                                                                                                  // (brillo)
        renderizador.dibujarRect(x + w * 0.35f, centerY, 0f, 0f, 0f, w * 0.3f, alto, 0.1f, 0.5f, 0.1f, 1.0f); // Derecha
                                                                                                              // muy
                                                                                                              // oscura

        // Sombrero (Pestaña)
        float capY = isTop ? (centerY - alto / 2 + capH / 2) : (centerY + alto / 2 - capH / 2);

        renderizador.dibujarRect(x - capW * 0.25f, capY, 0f, 0f, 0f, capW * 0.5f, capH, 0.2f, 0.6f, 0.2f, 1.0f);
        renderizador.dibujarRect(x, capY, 0f, 0f, 0f, capW * 0.4f, capH, 0.5f, 0.9f, 0.4f, 1.0f);
        renderizador.dibujarRect(x + capW * 0.35f, capY, 0f, 0f, 0f, capW * 0.3f, capH, 0.1f, 0.5f, 0.1f, 1.0f);
    }

    private void render() {
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(programa);

        // 1. Fondo Texturizado
        renderizador.dibujarTextura(texFondo, 0.0f, 0.0f, 2.0f, 2.0f, 1.0f);

        // 2. Suelo (Franja marrón con pasto verde)
        renderizador.dibujarRect(0.0f, -0.9f, 0f, 0f, 0f, 2.0f, 0.2f, 0.6f, 0.4f, 0.2f, 1.0f); // Marrón
        renderizador.dibujarRect(0.0f, -0.78f, 0f, 0f, 0f, 2.0f, 0.04f, 0.4f, 0.8f, 0.3f, 1.0f); // Pasto

        // 3. Tuberías 3D
        for (Tuberia t : tuberias) {
            float gapTop = t.gapCentroY + (GAP_ALTO * 0.5f);
            float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);

            float altoSuperior = 1.0f - gapTop;
            if (altoSuperior > 0.0f) {
                float yCentroSup = gapTop + (altoSuperior * 0.5f);
                dibujarTuberia3D(t.x, yCentroSup, altoSuperior, true);
            }

            float altoInferior = gapBottom + 1.0f;
            // Limitamos a que no cubra el suelo -0.8
            float sueloTop = -0.8f;
            if (gapBottom > sueloTop) {
                float hInf = gapBottom - sueloTop;
                float yCentroInf = sueloTop + (hInf * 0.5f);
                dibujarTuberia3D(t.x, yCentroInf, hInf, false);
            }
        }

        // 4. Pájaros
        j1.dibujar(renderizador);
        j2.dibujar(renderizador);

        // 5. HUD
        // Panel Izquierdo J1
        renderizador.dibujarRect(-0.7f, 0.85f, 0f, 0f, 0f, 0.5f, 0.2f, 0.1f, 0.1f, 0.1f, 0.6f);
        renderizador.dibujarTextRenderer(trHud1, -0.7f, 0.85f, 0.5f, 0.25f, 1.0f);

        // Panel Derecho J2
        renderizador.dibujarRect(0.7f, 0.85f, 0f, 0f, 0f, 0.5f, 0.2f, 0.1f, 0.1f, 0.1f, 0.6f);
        renderizador.dibujarTextRenderer(trHud2, 0.7f, 0.85f, 0.5f, 0.25f, 1.0f);

        // Panel Central HUD (Nivel)
        renderizador.dibujarRect(0.0f, 0.9f, 0f, 0f, 0f, 0.25f, 0.15f, 0.1f, 0.1f, 0.1f, 0.7f);
        for (int i = 0; i < nivelActual; i++) {
            float offset = (i - (nivelActual - 1) / 2.0f) * 0.035f;
            renderizador.dibujarRect(offset, 0.9f, 0f, 0f, 0f, 0.02f, 0.08f, 0.3f, 0.8f, 1.0f, 1.0f);
        }

        // 6. Pantallas UI (Inicio y GameOver)
        if (!started) {
            renderizador.dibujarRect(0.0f, 0.2f, 0f, 0f, 0f, 1.2f, 0.4f, 0.0f, 0.0f, 0.0f, 0.6f);
            renderizador.dibujarTextRenderer(trTitle, 0.0f, 0.2f, 1.0f, 0.25f, 1.0f);

            // X roja
            renderizador.dibujarRect(0.0f, 0.0f, (float) Math.PI / 4, 0f, 0f, 0.05f, 0.2f, 1.0f, 0.2f, 0.2f, 1.0f);
            renderizador.dibujarRect(0.0f, 0.0f, -(float) Math.PI / 4, 0f, 0f, 0.05f, 0.2f, 1.0f, 0.2f, 0.2f, 1.0f);
            trGameOver.renderText("Presiona SPACE o W para empezar", 28, Color.LIGHT_GRAY, true, 1.0f);
            renderizador.dibujarTextRenderer(trGameOver, 0.0f, -0.2f, 1.0f, 0.5f, 1.0f);
        } else if (gameOver) {
            // Panel Game Over
            renderizador.dibujarRect(0.0f, 0.1f, 0f, 0f, 0f, 1.4f, 0.8f, 0.1f, 0.05f, 0.05f, 0.85f);

            // X roja gigante
            renderizador.dibujarRect(0.0f, 0.35f, (float) Math.PI / 4, 0f, 0f, 0.08f, 0.3f, 1.0f, 0.2f, 0.2f, 1.0f);
            renderizador.dibujarRect(0.0f, 0.35f, -(float) Math.PI / 4, 0f, 0f, 0.08f, 0.3f, 1.0f, 0.2f, 0.2f, 1.0f);

            // Puntajes y texto
            renderizador.dibujarTextRenderer(trGameOver, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f);

            // Reusando trTitle para "Presiona R para reiniciar" de manera hacky o
            // simplemente crear otro string
            trTitle.renderText("Presiona R para reiniciar", 30, Color.WHITE, true, 1.0f);
            renderizador.dibujarTextRenderer(trTitle, 0.0f, -0.2f, 1.0f, 0.25f, 1.0f);
        }
    }

    private void actualizarTitulo() {
        String tituloBase = String.format("Flappy Bird | Nivel: %d | Vel: %.2f", nivelActual, velocidadTuberias);
        if (!started) {
            GLFW.glfwSetWindowTitle(window, tituloBase + " | SPACE/W empezar");
        } else if (gameOver) {
            GLFW.glfwSetWindowTitle(window, tituloBase + " | GAME OVER");
        } else {
            GLFW.glfwSetWindowTitle(window, tituloBase);
        }
    }

    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt = ahora - ultimoTiempo;
            ultimoTiempo = ahora;
            if (dt > 0.033f)
                dt = 0.033f;

            procesarInput();
            actualizar(dt);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        if (renderizador != null)
            renderizador.cleanup();
        if (texFondo != null)
            texFondo.cleanup();
        if (trHud1 != null)
            trHud1.cleanup();
        if (trHud2 != null)
            trHud2.cleanup();
        if (trGameOver != null)
            trGameOver.cleanup();
        if (trTitle != null)
            trTitle.cleanup();

        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new AppFlappyBird().run();
    }
}
