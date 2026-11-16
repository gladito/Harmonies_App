package com.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio08 extends BaseTest {

    @Test
    void navegarEntrePaginasConLinksGoBackYReload() {
        // Precondición: Iniciar sesión en Harmony para habilitar la barra de navegación
        page.navigate(BASE_URL + "/login");
        page.getByLabel("Email").fill("gladito@example.test");
        page.getByLabel("Contraseña").fill("Harmony123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar sesión")).click();

        // Paso 1: navigate al inicio (/home) y hasTitle() del sitio
        page.navigate(BASE_URL + "/home");
        assertThat(page).hasTitle("Harmony · Comunidad");

        // Paso 2: clic en un link a la página A (Salas) y hasURL de esa página
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Salas")).click();
        assertThat(page).hasURL(BASE_URL + "/studios");

        // Paso 3: page.goBack() y hasURL del inicio (/home)
        page.goBack();
        assertThat(page).hasURL(BASE_URL + "/home");

        // Paso 4: clic en un link a la página B (Trabajos) y hasURL de esa página
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Trabajos")).click();
        assertThat(page).hasURL(BASE_URL + "/jobs");

        // Paso 5: page.reload() y hasURL de la página B sin cambios
        page.reload();
        assertThat(page).hasURL(BASE_URL + "/jobs");
    }
}
