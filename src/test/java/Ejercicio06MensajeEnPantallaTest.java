import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ejercicio06MensajeEnPantallaTest extends BaseTest {

    @Test
    void test_mensajeDeExitoAlIniciarSesion() {
        // 1. Navegar a la página de login de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Verificar que todavía no hay ningún mensaje de error en pantalla
        Locator errorAlert = page.locator(".alert.error");
        assertFalse(errorAlert.isVisible(), "Antes de enviar el formulario no debería haber alerta de error");

        // 3. Completar el formulario con credenciales válidas de Harmony y enviarlo
        page.locator("#email").fill("gladito@example.test");
        page.locator("#password").fill("Harmony123");
        page.locator("button[type='submit']").click();

        // 4. Verificar que apareció el mensaje de bienvenida personalizado en /home
        Locator welcomeMessage = page.locator("section.hero p.muted");
        assertTrue(welcomeMessage.isVisible(), "Después del login debería mostrarse el mensaje de bienvenida en /home");

        // 5. Verificar el texto del mensaje de bienvenida
        String textoDelMensaje = welcomeMessage.innerText().trim();
        assertTrue(textoDelMensaje.contains("Bienvenido, gladito"),
                "El mensaje debería dar la bienvenida al usuario gladito, pero decía: " + textoDelMensaje);
    }

    @Test
    void test_mensajeDeErrorConCredencialesInvalidas() {
        // 1. Navegar a la página de login de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Completar el formulario con credenciales inválidas y enviarlo
        page.locator("#email").fill("usuarioInvalido@example.test");
        page.locator("#password").fill("claveInvalida");
        page.locator("button[type='submit']").click();

        // 3. Verificar que aparece la alerta de error en pantalla
        Locator errorAlert = page.locator(".alert.error");
        assertTrue(errorAlert.isVisible(), "Con credenciales inválidas debería aparecer un mensaje de error en Harmony");

        // 4. Verificar el texto del mensaje y que no se entró a /home
        String textoDelMensaje = errorAlert.innerText().trim();
        assertTrue(textoDelMensaje.contains("Email o contraseña incorrectos."),
                "El mensaje debería avisar que las credenciales son inválidas, pero decía: " + textoDelMensaje);
        assertFalse(page.url().contains("/home"), "Con credenciales inválidas no deberíamos llegar a /home");
    }
}
