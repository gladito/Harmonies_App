package com.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;

public class BaseTest {

    public static final String BASE_URL = System.getProperty("harmony.url", "http://127.0.0.1:8080");
    private static Process serverProcess;

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

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
            return conn.getResponseCode() == 200;
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

        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
        context.close();
        browser.close();
        playwright.close();
    }
}
