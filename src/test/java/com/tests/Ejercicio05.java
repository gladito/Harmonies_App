package com.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio05 extends BaseTest {

    @Test
    void loginExitoso() {
        // Paso 1: Navegar al formulario de inicio de sesión de Harmony
        page.navigate(BASE_URL + "/login");

        // Paso 2: Completar email y contraseña válidos
        page.getByLabel("Email").fill("gladito@example.test");
        page.getByLabel("Contraseña").fill("Harmony123");

        // Paso 3: Hacer clic en el botón de envío
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar sesión")).click();

        // Paso 4: Verificar con hasURL que redirige a la página de éxito (/home)
        assertThat(page).hasURL(BASE_URL + "/home");
    }

    @Test
    void loginFallido() {
        // Paso 1: Navegar de forma independiente al formulario de inicio de sesión
        page.navigate(BASE_URL + "/login");

        // Paso 2: Completar con credenciales inválidas
        page.getByLabel("Email").fill("usuario_inexistente@example.test");
        page.getByLabel("Contraseña").fill("ClaveIncorrecta99");

        // Paso 3: Hacer clic en el botón de envío
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar sesión")).click();

        // Paso 4: Verificar que permanece en el formulario de login (?error) y no entra a /home
        assertThat(page).hasURL(BASE_URL + "/login?error");
    }
}
