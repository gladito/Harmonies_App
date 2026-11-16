package pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class DashboardPage {

    private final Page page;
    private final Locator welcomeMessage;
    private final Locator logoutButton;
    private final Locator heroHeader;

    public DashboardPage(Page page) {
        this.page = page;
        this.welcomeMessage = page.locator("section.hero p.muted");
        this.logoutButton = page.locator("nav form[action='/logout'] button");
        this.heroHeader = page.locator("section.hero h1");
    }

    public boolean isLoggedIn() {
        return page.url().contains("/home")
                && logoutButton.isVisible();
    }

    public String getSuccessMessage() {
        return welcomeMessage.innerText().trim();
    }

    public String getHeaderText() {
        return heroHeader.innerText().trim();
    }

    public LoginPage logout() {
        logoutButton.click();
        return new LoginPage(page);
    }
}
