import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ejercicio08NavegacionTest extends BaseTest {

    @Test
    void test_navegarAdelanteYAtras() {
        iniciarSesionEnHarmony();

        String urlPrincipal = BASE_URL + "/home";
        page.navigate(urlPrincipal);
        page.waitForTimeout(1000);

        // Hacer clic en el enlace "Salas" de la barra de navegación
        Locator linkSalas = page.locator("nav a[href='/studios']");
        linkSalas.click();
        page.waitForTimeout(1000);

        String urlDestino = BASE_URL + "/studios";
        assertEquals(urlDestino, page.url(), "La URL debería ser la de Salas (/studios)");
        assertTrue(page.locator("section.hero h1").innerText().contains("Encontrá dónde sonar."),
                "El título de Salas de ensayo debería estar visible");

        // Volver atrás al Feed
        page.goBack();
        page.waitForTimeout(1000);

        assertEquals(urlPrincipal, page.url(), "Deberíamos haber vuelto al Feed (/home)");
        assertTrue(linkSalas.isVisible(), "El enlace a Salas debería estar visible nuevamente en la barra");

        // Avanzar hacia adelante nuevamente a Salas
        page.goForward();
        page.waitForTimeout(1000);
        assertEquals(urlDestino, page.url(), "Deberíamos haber avanzado a la pantalla de Salas (/studios) nuevamente");
    }
}
