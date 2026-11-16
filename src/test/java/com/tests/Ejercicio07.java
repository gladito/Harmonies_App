package com.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

public class Ejercicio07 extends BaseTest {

    @Test
    void tomarCapturasDePantallaComoEvidencia() {
        // 1. Captura 1: Pantalla de Login de Harmony (raíz del proyecto)
        page.navigate(BASE_URL + "/login");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("captura01_login.png")));

        // 2. Iniciar sesión para acceder al Feed principal de Harmony
        page.getByLabel("Email").fill("gladito@example.test");
        page.getByLabel("Contraseña").fill("Harmony123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Iniciar sesión")).click();

        // 3. Captura 2: Página completa del Feed con setFullPage(true) en la raíz del proyecto
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("captura02_home_completa.png"))
                .setFullPage(true));

        // 4. Captura 3: Pantalla de Salas de ensayo en la raíz del proyecto
        page.navigate(BASE_URL + "/studios");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("captura03_salas.png"))
                .setFullPage(true));

        // 5. Imprimir mensaje de confirmación en consola
        System.out.println("Se generaron correctamente las 3 capturas PNG en la raíz del proyecto.");
    }
}
