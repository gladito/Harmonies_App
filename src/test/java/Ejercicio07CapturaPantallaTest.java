import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Ejercicio07CapturaPantallaTest extends BaseTest {

    @Test
    void test_tomarCapturaDePantalla() {
        iniciarSesionEnHarmony();
        page.navigate(BASE_URL + "/home");

        page.waitForTimeout(1000);

        Path screenshotPath = Paths.get("screenshots/ejercicio07_captura.png");

        page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshotPath)
                .setFullPage(true));

        assertTrue(Files.exists(screenshotPath), "El archivo de la captura de pantalla de Harmony debería existir");
    }
}
