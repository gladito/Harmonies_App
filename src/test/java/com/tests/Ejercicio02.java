package com.tests;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio02 extends BaseTest {

    @Test
    void verificarTituloExacto() {
        // 1. Navegar a la pantalla de inicio de sesión de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Verificar el título exacto real del sitio
        assertThat(page).hasTitle("Iniciar sesión");
    }

    @Test
    void verificarTituloConExpresionRegular() {
        // 1. Navegar a la pantalla de inicio de sesión de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Verificar una parte del título usando Pattern.compile
        assertThat(page).hasTitle(Pattern.compile("Iniciar.*"));
    }
}
