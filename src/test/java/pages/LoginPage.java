package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LoginPage {

    private static final String URL = System.getProperty("harmony.url", "http://127.0.0.1:8080") + "/login";

    private final Page page;
    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator loginButton;
    private final Locator errorMessage;

    public LoginPage(Page page) {
        this.page = page;
        this.emailInput = page.locator("#email");
        this.passwordInput = page.locator("#password");
        this.loginButton = page.locator("button[type='submit']");
        this.errorMessage = page.locator(".alert.error");
    }

    public LoginPage navigate() {
        page.navigate(URL);
        return this;
    }

    public DashboardPage login(String email, String password) {
        emailInput.fill(email);
        passwordInput.fill(password);
        loginButton.click();
        return new DashboardPage(page);
    }

    public String getFlashMessage() {
        return errorMessage.first().innerText().trim();
    }

    public boolean isDisplayed() {
        return emailInput.isVisible() && passwordInput.isVisible();
    }
}
