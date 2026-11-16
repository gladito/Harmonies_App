package com.tests;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio02 extends BaseTest {

    @Test
    void verificarTituloExacto() {
        // 1. Navegar a la portada principal (index) de Harmony
        page.navigate(BASE_URL + "/");

        // 2. Verificar el título exacto real del index
        assertThat(page).hasTitle("Harmony · Red Colaborativa de Músicos y Bandas");
    }

    @Test
    void verificarTituloConExpresionRegular() {
        // 1. Navegar a la portada principal (index) de Harmony
        page.navigate(BASE_URL + "/");

        // 2. Verificar una parte del título usando Pattern.compile
        assertThat(page).hasTitle(Pattern.compile("Harmony.*"));
    }
}
