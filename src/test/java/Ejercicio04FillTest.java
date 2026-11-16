import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Ejercicio04FillTest extends BaseTest {

    @Test
    void test_escribirTextoEnCampo() {
        // 1. Navegar a la pantalla de inicio de sesión de Harmony
        page.navigate(BASE_URL + "/login");

        // 2. Localizar el campo de texto del email
        Locator emailInput = page.locator("#email");

        // 3. Escribir texto (fill) en el campo con una cuenta de Harmony
        String correoDePrueba = "gladito@example.test";
        emailInput.fill(correoDePrueba);

        // 4. Verificar que el texto se ingresó correctamente
        String valorIngresado = emailInput.inputValue();
        assertEquals(correoDePrueba, valorIngresado, "El texto dentro del campo #email debería coincidir con lo que escribimos");

        // 5. Probar también rellenar el buscador de etiquetas musicales en el Feed (/home)
        iniciarSesionEnHarmony();
        Locator tagFilterInput = page.locator("#tag");
        tagFilterInput.fill("synthwave");
        assertEquals("synthwave", tagFilterInput.inputValue(), "El filtro de etiquetas debería contener 'synthwave'");
    }
}
