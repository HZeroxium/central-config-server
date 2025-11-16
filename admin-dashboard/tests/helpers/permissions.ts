import { Page, expect } from '@playwright/test';
import { ROUTES, isAdminOnlyRoute } from '../constants/routes';

/**
 * Permission checking helpers
 */
export class PermissionHelper {
  constructor(private page: Page) {}

  /**
   * Check if current page is unauthorized
   */
  async isUnauthorizedPage(): Promise<boolean> {
    const url = this.page.url();
    return url.includes(ROUTES.UNAUTHORIZED);
  }

  /**
   * Verify user is redirected to unauthorized page
   */
  async verifyUnauthorizedRedirect(): Promise<void> {
    await this.page.waitForURL(`**${ROUTES.UNAUTHORIZED}`, { timeout: 10000 });
    await expect(this.page.locator('body')).toContainText(/unauthorized|access denied/i);
  }

  /**
   * Verify user can access route (not redirected to unauthorized)
   */
  async verifyCanAccessRoute(route: string): Promise<void> {
    await this.page.goto(route);
    await this.page.waitForTimeout(2000); // Wait for potential redirect
    
    const currentUrl = this.page.url();
    expect(currentUrl).not.toContain(ROUTES.UNAUTHORIZED);
    expect(currentUrl).toContain(route);
  }

  /**
   * Verify user cannot access route (redirected to unauthorized)
   */
  async verifyCannotAccessRoute(route: string): Promise<void> {
    await this.page.goto(route);
    await this.verifyUnauthorizedRedirect();
  }

  /**
   * Check if route requires admin access
   */
  isAdminRoute(route: string): boolean {
    return isAdminOnlyRoute(route);
  }

  /**
   * Verify admin-only UI elements are visible/hidden based on role
   */
  async verifyAdminOnlyElementVisible(selector: string, shouldBeVisible: boolean): Promise<void> {
    const element = this.page.locator(selector).first();
    if (shouldBeVisible) {
      await expect(element).toBeVisible({ timeout: 5000 });
    } else {
      await expect(element).not.toBeVisible({ timeout: 5000 });
    }
  }
}

