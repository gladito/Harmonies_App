package com.tests;

import org.junit.jupiter.api.Test;

public class Ejercicio01 extends BaseTest {

    @Test
    void abrirNavegadorYNavegarAHarmony() {
        // 1. Navegar a la URL real del proyecto Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Imprimir el título de la página en consola
        System.out.println("Título de la página: " + page.title());
    }
}
