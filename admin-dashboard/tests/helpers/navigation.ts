import { Page, expect } from '@playwright/test';

/**
 * Navigation helper utilities
 */
export class NavigationHelper {
  constructor(private page: Page) {}

  /**
   * Wait for Keycloak redirect to complete
   */
  async waitForKeycloakRedirect(baseUrl: string, timeout = 30000): Promise<void> {
    await this.page.waitForURL(new RegExp(baseUrl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), {
      timeout,
      waitUntil: 'networkidle',
    });
  }

  /**
   * Check if we're on Keycloak login page
   */
  async isKeycloakLoginPage(): Promise<boolean> {
    const url = this.page.url();
    return url.includes('/realms/') && url.includes('/protocol/openid-connect/auth');
  }

  /**
   * Check if we're on the application
   */
  async isApplication(baseUrl: string): Promise<boolean> {
    const url = this.page.url();
    return url.startsWith(baseUrl) && !url.includes('/realms/');
  }

  /**
   * Wait for page to be fully loaded (no network requests)
   */
  async waitForPageReady(): Promise<void> {
    await this.page.waitForLoadState('networkidle');
    await this.page.waitForLoadState('domcontentloaded');
  }

  /**
   * Verify URL contains expected path
   */
  async verifyUrlContains(path: string): Promise<void> {
    const url = this.page.url();
    expect(url).toContain(path);
  }

  /**
   * Verify current route matches expected
   */
  async verifyRoute(expectedRoute: string): Promise<void> {
    const currentPath = new URL(this.page.url()).pathname;
    expect(currentPath).toBe(expectedRoute);
  }

  /**
   * Get all cookies
   */
  async getCookies() {
    return await this.page.context().cookies();
  }

  /**
   * Check if cookie exists
   */
  async hasCookie(name: string): Promise<boolean> {
    const cookies = await this.getCookies();
    return cookies.some(cookie => cookie.name === name);
  }

  /**
   * Get cookie value
   */
  async getCookieValue(name: string): Promise<string | undefined> {
    const cookies = await this.getCookies();
    const cookie = cookies.find(c => c.name === name);
    return cookie?.value;
  }

  /**
   * Wait for element to be visible with timeout
   */
  async waitForElement(selector: string, timeout = 10000): Promise<void> {
    await this.page.waitForSelector(selector, { state: 'visible', timeout });
  }

  /**
   * Wait for element to be hidden
   */
  async waitForElementHidden(selector: string, timeout = 10000): Promise<void> {
    await this.page.waitForSelector(selector, { state: 'hidden', timeout });
  }

  /**
   * Check if element exists (without waiting)
   */
  async elementExists(selector: string): Promise<boolean> {
    try {
      const element = this.page.locator(selector);
      await element.waitFor({ state: 'attached', timeout: 2000 });
      return true;
    } catch {
      return false;
    }
  }
}

