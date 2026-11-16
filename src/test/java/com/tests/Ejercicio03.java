package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio03 extends BaseTest {

    @Test
    void hacerClicEnLinkDeLoginDesdeIndex() {
        // 1. Navegar a la portada principal (index) de Harmony
        page.navigate(BASE_URL + "/");

        // 2. Localizar el botón/link "Iniciar sesión →" en el index usando getByRole(AriaRole.LINK) y setName(...)
        Locator linkIniciarSesion = page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Iniciar sesión →")
        ).first();

        // 3. Ejecutar click() sobre el link para ir al Login
        linkIniciarSesion.click();

        // 4. Verificar con assertThat(page).hasURL() que llegó a la pantalla de login (/login)
        assertThat(page).hasURL(BASE_URL + "/login");
    }
}
