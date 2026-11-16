package com.tests;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Ejercicio04 extends BaseTest {

    @Test
    void escribirTextoEnCamposDeFormulario() {
        // 1. Navegar al formulario de inicio de sesión de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Localizar los dos campos usando getByLabel (sin selectores CSS #id ni .clase)
        Locator campoEmail = page.getByLabel("Email");
        Locator campoPassword = page.getByLabel("Contraseña");

        // 3. Completar el primer y segundo campo con fill()
        campoEmail.fill("gladito@example.test");
        campoPassword.fill("Harmony123");

        // 4. Obtener los valores con inputValue() e imprimirlos en consola
        String emailIngresado = campoEmail.inputValue();
        String passwordIngresado = campoPassword.inputValue();

        System.out.println("Email ingresado: " + emailIngresado);
        System.out.println("Contraseña ingresada: " + passwordIngresado);

        assertEquals("gladito@example.test", emailIngresado);
        assertEquals("Harmony123", passwordIngresado);
    }
}
