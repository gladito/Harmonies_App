import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Paths;

public class BaseTest {

    public static final String BASE_URL = System.getProperty("harmony.url", "http://127.0.0.1:8080");
    private static Process serverProcess;

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;
    protected boolean testFailed = false;

    @RegisterExtension
    TestWatcher watchman = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext extensionContext, Throwable cause) {
            testFailed = true;
        }
    };

    @BeforeAll
    static synchronized void ensureHarmonyServerIsRunning() throws Exception {
        if (isServerUp()) {
            return;
        }
        File jarFile = new File("C:\\Users\\Agustin\\Desktop\\Todo de harmony\\HARMONY_BETA_HOY\\harmony.jar");
        if (jarFile.exists() && (serverProcess == null || !serverProcess.isAlive())) {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            ProcessBuilder pb = new ProcessBuilder(
                    javaBin, "-jar", jarFile.getAbsolutePath(), "--spring.profiles.active=demo"
            );
            pb.directory(jarFile.getParentFile());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            serverProcess = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (serverProcess != null && serverProcess.isAlive()) {
                    serverProcess.destroy();
                }
            }));
            for (int i = 0; i < 60; i++) {
                if (isServerUp()) {
                    return;
                }
                Thread.sleep(500);
            }
        }
    }

    private static boolean isServerUp() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(BASE_URL + "/login").toURL().openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        ensureHarmonyServerIsRunning();
        playwright = Playwright.create();

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(headless)
        );

        context = browser.newContext(new Browser.NewContextOptions()
                .setRecordVideoDir(Paths.get("videos/"))
                .setRecordVideoSize(1280, 720));

        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
    }

    protected void iniciarSesionEnHarmony() {
        page.navigate(BASE_URL + "/login");
        page.locator("#email").fill("gladito@example.test");
        page.locator("#password").fill("Harmony123");
        page.locator("button[type='submit']").click();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        if (testFailed) {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("screenshots/" + testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".png")));
        }

        context.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("traces/" + testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".zip")));

        context.close();
        browser.close();
        playwright.close();
    }
}
