import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Keycloak login page object
 * Handles Keycloak authentication flow
 */
export class KeycloakLoginPage extends BasePage {
  // Selectors for Keycloak login page
  readonly usernameInput = () => this.page.locator('#username');
  readonly passwordInput = () => this.page.locator('#password');
  readonly loginButton = () => this.page.locator('#kc-login');
  readonly errorMessage = () => this.page.locator('.alert-error');
  readonly keycloakLogo = () => this.page.locator('#kc-logo');

  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to Keycloak login page
   * Usually redirected here from the application
   */
  async goto(): Promise<void> {
    // Keycloak login page is typically accessed via redirect
    // If direct access needed, use the Keycloak URL
    const keycloakUrl = process.env.KEYCLOAK_URL || 'http://localhost:8080';
    await this.page.goto(`${keycloakUrl}/realms/config-control/protocol/openid-connect/auth`);
    await this.waitForPageLoad();
  }
  
  /**
   * Wait for Keycloak page to load
   */
  async waitForKeycloakPage(): Promise<void> {
    await this.page.waitForSelector('#username', { state: 'visible', timeout: 10000 });
    await this.page.waitForSelector('#password', { state: 'visible', timeout: 10000 });
  }

  /**
   * Check if we're on the Keycloak login page
   */
  async isLoginPage(): Promise<boolean> {
    return await this.isVisible('#username') && await this.isVisible('#password');
  }

  /**
   * Fill in username
   */
  async fillUsername(username: string): Promise<void> {
    await this.usernameInput().fill(username);
  }

  /**
   * Fill in password
   */
  async fillPassword(password: string): Promise<void> {
    await this.passwordInput().fill(password);
  }

  /**
   * Click login button
   */
  async clickLogin(): Promise<void> {
    await this.loginButton().click();
  }

  /**
   * Perform login with credentials
   */
  async login(username: string, password: string): Promise<void> {
    await this.fillUsername(username);
    await this.fillPassword(password);
    await this.clickLogin();
    // Wait for redirect back to application
    await this.waitForNavigation();
  }

  /**
   * Check if there's an error message
   */
  async hasError(): Promise<boolean> {
    return await this.isVisible('.alert-error');
  }

  /**
   * Get error message text
   */
  async getErrorMessage(): Promise<string | null> {
    if (await this.hasError()) {
      return await this.errorMessage().textContent();
    }
    return null;
  }

  /**
   * Wait for redirect back to application after login
   */
  async waitForRedirect(baseUrl: string, timeout = 30000): Promise<void> {
    await this.page.waitForURL(new RegExp(baseUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), {
      timeout,
      waitUntil: 'networkidle',
    });
  }
}

