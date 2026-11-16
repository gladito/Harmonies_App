package com.tests;

import org.junit.jupiter.api.Test;

public class Ejercicio01 extends BaseTest {

    @Test
    void abrirNavegadorYNavegarAHarmony() {
        // 1. Navegar a la portada principal (index) del proyecto Harmony
        page.navigate(BASE_URL + "/");

        // 2. Imprimir el título de la página en consola
        System.out.println("Título de la página: " + page.title());
    }
}
