package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio03 extends BaseTest {

    @Test
    void hacerClicEnLinkDeRegistro() {
        // 1. Navegar a la página de login de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Localizar el link real del sitio usando getByRole(AriaRole.LINK) y setName(...)
        Locator linkCrearCuenta = page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions().setName("Crea una ahora")
        );

        // 3. Ejecutar click() sobre el link
        linkCrearCuenta.click();

        // 4. Verificar con assertThat(page).hasURL() la URL de la página destino
        assertThat(page).hasURL(BASE_URL + "/register");
    }
}
