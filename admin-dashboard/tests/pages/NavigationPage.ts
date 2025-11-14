import { Page } from '@playwright/test';
import { BasePage } from './BasePage';

/**
 * Navigation helper page object
 * Handles navigation between pages in the application
 */
export class NavigationPage extends BasePage {
  // Sidebar navigation selectors
  readonly sidebar = () => this.page.locator('[data-testid="sidebar"], nav, aside').first();
  readonly sidebarToggle = () => this.page.locator('[data-testid="sidebar-toggle"], button[aria-label*="menu"]');
  
  // Navigation link selectors
  readonly navLink = (href: string) => this.page.locator(`a[href="${href}"]`);
  readonly navItem = (text: string) => this.page.locator(`text="${text}"`).first();

  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to a route by clicking sidebar link
   */
  async navigateTo(route: string): Promise<void> {
    const link = this.navLink(route);
    await link.click();
    await this.waitForNavigation();
  }

  /**
   * Navigate to a route by clicking link with specific text
   */
  async navigateByText(text: string): Promise<void> {
    const link = this.navItem(text);
    await link.click();
    await this.waitForNavigation();
  }

  /**
   * Check if current route matches expected
   */
  isRoute(expectedRoute: string): boolean {
    const url = this.getUrl();
    return url.includes(expectedRoute);
  }

  /**
   * Toggle sidebar (if collapsible)
   */
  async toggleSidebar(): Promise<void> {
    if (await this.isVisible('[data-testid="sidebar-toggle"], button[aria-label*="menu"]')) {
      await this.sidebarToggle().click();
      await this.page.waitForTimeout(300); // Wait for animation
    }
  }

  /**
   * Get current route path
   */
  getCurrentRoute(): string {
    const url = new URL(this.getUrl());
    return url.pathname;
  }

  /**
   * Check if navigation link is active/selected
   */
  async isNavLinkActive(route: string): Promise<boolean> {
    const link = this.navLink(route);
    const classList = await link.getAttribute('class');
    return classList?.includes('active') || classList?.includes('Mui-selected') || false;
  }

  /**
   * Wait for route change
   */
  async waitForRouteChange(currentRoute: string, timeout = 10000): Promise<void> {
    await this.page.waitForFunction(
      (route) => !window.location.pathname.includes(route),
      currentRoute,
      { timeout }
    );
  }
}

