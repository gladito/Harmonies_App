package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio06 extends BaseTest {

    @Test
    void verificarMensajeDeExitoEnPantalla() {
        // Paso 1: Navegar al login de Harmony
        page.navigate(BASE_URL + "/login");

        // Paso 2: Declarar el Locator reutilizable para el contenedor de mensajes de estado
        Locator contenedorMensaje = page.locator(".alert.error, section.hero p.muted");

        // Paso 3: Iniciar sesión con credenciales válidas
        page.getByLabel("Email").fill("gladito@example.test");
        page.getByLabel("Contraseña").fill("Harmony123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar sesión")).click();

        // Paso 4: Verificar que el mensaje es visible y contiene el texto real de éxito de Harmony
        assertThat(contenedorMensaje).isVisible();
        assertThat(contenedorMensaje).containsText("Bienvenido, gladito. Compartí tu sonido y encontrá tu banda.");
    }

    @Test
    void verificarMensajeDeErrorEnPantalla() {
        // Paso 1: Navegar al login de Harmony
        page.navigate(BASE_URL + "/login");

        // Paso 2: Reutilizar el Locator para el contenedor de mensajes de estado
        Locator contenedorMensaje = page.locator(".alert.error, section.hero p.muted");

        // Paso 3: Intentar iniciar sesión con credenciales inválidas
        page.getByLabel("Email").fill("invalido@example.test");
        page.getByLabel("Contraseña").fill("ClaveErronea123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar sesión")).click();

        // Paso 4: Verificar que el mensaje de error es visible y contiene el texto real del sitio
        assertThat(contenedorMensaje).isVisible();
        assertThat(contenedorMensaje).containsText("Email o contraseña incorrectos.");
    }
}
