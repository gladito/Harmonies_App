import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ejercicio05LoginCompletoTest extends BaseTest {

    @Test
    void test_loginCompleto() {
        page.navigate(BASE_URL + "/login");

        Locator emailInput = page.locator("#email");
        Locator passwordInput = page.locator("#password");
        Locator loginButton = page.locator("button[type='submit']");

        String email = "gladito@example.test";
        String clave = "Harmony123";
        emailInput.fill(email);
        passwordInput.fill(clave);

        assertEquals(email, emailInput.inputValue(), "El email debería coincidir con lo que escribimos");
        assertEquals(clave, passwordInput.inputValue(), "La clave debería coincidir con lo que escribimos");

        loginButton.click();

        assertTrue(page.url().contains("/home"), "Después del login la URL debería ser /home");

        Locator homeHeader = page.locator("section.hero h1");
        assertEquals("Tu próxima colaboración empieza acá.", homeHeader.innerText().trim(), "El encabezado principal de Harmony debería coincidir");

        Locator logoutButton = page.locator("nav form[action='/logout'] button");
        assertTrue(logoutButton.isVisible(), "El botón de Salir debería ser visible una vez logueados en Harmony");
    }
}
