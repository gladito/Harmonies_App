import org.junit.jupiter.api.Test;
import pages.DashboardPage;
import pages.LoginPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginTest extends BaseTest {

    private static final String VALID_EMAIL = "gladito@example.test";
    private static final String VALID_PASSWORD = "Harmony123";
    private static final String INVALID_EMAIL = "usuarioInvalido@example.test";
    private static final String INVALID_PASSWORD = "claveInvalida";

    @Test
    void test_loginExitoso() {
        LoginPage loginPage = new LoginPage(page).navigate();

        DashboardPage dashboard = loginPage.login(VALID_EMAIL, VALID_PASSWORD);

        assertTrue(dashboard.isLoggedIn(), "El usuario debería haber ingresado a /home");
        assertTrue(dashboard.getSuccessMessage().contains("gladito"), "El mensaje de bienvenida debería incluir al usuario gladito");
        assertTrue(dashboard.getHeaderText().contains("Tu próxima colaboración empieza acá."));
    }

    @Test
    void test_loginFallido() {
        LoginPage loginPage = new LoginPage(page).navigate();

        loginPage.login(INVALID_EMAIL, INVALID_PASSWORD);

        assertFalse(page.url().contains("/home"), "No debería ingresar a /home con credenciales inválidas");
        assertTrue(loginPage.getFlashMessage().contains("Email o contraseña incorrectos."));
    }
}
