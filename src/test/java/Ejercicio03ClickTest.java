import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ejercicio03ClickTest extends BaseTest {

    @Test
    void test_hacerClicEnBoton() {
        // 1. Iniciar sesión en Harmony e ir a la sección de Salas de ensayo
        iniciarSesionEnHarmony();
        page.navigate(BASE_URL + "/studios");

        // 2. Localizar el botón desplegable "Publicar una sala" y el campo "#name" dentro del formulario
        Locator botonPublicarSala = page.locator("details summary");
        Locator campoNombreSala = page.locator("#name");

        // 3. Hacer clic para desplegar el formulario de publicación de sala
        botonPublicarSala.click();

        // 4. Verificar que el formulario se desplegó y el campo Nombre es visible
        assertTrue(campoNombreSala.isVisible(), "El formulario para publicar una sala debería ser visible tras hacer clic");

        // 5. Hacer clic de nuevo para contraer el formulario y verificar que se oculta
        botonPublicarSala.click();
        assertTrue(campoNombreSala.isHidden(), "El formulario debería ocultarse tras volver a hacer clic");
    }
}
