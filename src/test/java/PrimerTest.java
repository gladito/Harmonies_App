import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrimerTest extends BaseTest {

    @Test
    void verificaTituloLogin() {
        page.navigate(BASE_URL + "/login");

        assertTrue(page.title().contains("Iniciar sesión"), "El título debería ser el de inicio de sesión de Harmony");
        assertEquals("Harmony", page.locator(".card h1").innerText().trim());
    }
}
