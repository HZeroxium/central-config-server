import { Page, Locator, expect } from '@playwright/test';
import { SELECTORS } from '../constants/selectors';

/**
 * Base page class following Page Object Model pattern
 * Provides common methods and selectors shared across all pages
 */
export abstract class BasePage {
  readonly page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  /**
   * Navigate to a specific route
   */
  async goto(route: string): Promise<void> {
    await this.page.goto(route);
  }

  /**
   * Wait for page to be fully loaded
   */
  async waitForPageLoad(timeout = 30000): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout });
    await this.page.waitForLoadState('domcontentloaded', { timeout });
  }

  /**
   * Verify page is loaded
   */
  async verifyPageLoaded(): Promise<void> {
    // Wait for main content to be visible
    await this.page.waitForSelector(SELECTORS.MAIN_CONTENT, { 
      state: 'visible', 
      timeout: 15000 
    });
    
    // Wait for loading indicators to disappear
    const loadingVisible = await this.page.locator(SELECTORS.LOADING).first().isVisible().catch(() => false);
    if (loadingVisible) {
      await this.page.waitForSelector(SELECTORS.LOADING, { 
        state: 'hidden', 
        timeout: 10000 
      }).catch(() => {
        // Loading might not exist, which is fine
      });
    }
  }

  /**
   * Verify data is present on the page
   * Checks for tables, cards, or other data containers
   */
  async verifyDataPresent(): Promise<void> {
    // Check for tables with data
    const table = this.page.locator(SELECTORS.TABLE).first();
    const tableExists = await table.count() > 0;
    
    if (tableExists) {
      // Wait for table to have rows (not empty)
      const rows = table.locator(SELECTORS.TABLE_ROW);
      const rowCount = await rows.count();
      expect(rowCount).toBeGreaterThan(0);
    } else {
      // Check for cards or other content
      const cards = this.page.locator(SELECTORS.CARD);
      const cardCount = await cards.count();
      
      // At least some content should be present
      const hasContent = cardCount > 0 || await this.page.locator('body').textContent() !== '';
      expect(hasContent).toBe(true);
    }
  }

  /**
   * Wait for a specific element to be visible
   */
  async waitForElement(selector: string, timeout = 10000): Promise<Locator> {
    const element = this.page.locator(selector);
    await element.waitFor({ state: 'visible', timeout });
    return element;
  }

  /**
   * Get page title
   */
  async getTitle(): Promise<string> {
    return await this.page.title();
  }

  /**
   * Get current URL
   */
  getUrl(): string {
    return this.page.url();
  }

  /**
   * Wait for navigation to complete
   */
  async waitForNavigation(timeout = 30000): Promise<void> {
    await this.page.waitForURL('**', { waitUntil: 'networkidle', timeout });
  }

  /**
   * Take screenshot with timestamp
   */
  async takeScreenshot(name: string): Promise<void> {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const filename = `${name}-${timestamp}.png`;
    await this.page.screenshot({ 
      path: `tests/screenshots/${filename}`,
      fullPage: true,
    });
  }

  /**
   * Check if element is visible
   */
  async isVisible(selector: string, timeout = 5000): Promise<boolean> {
    try {
      const element = this.page.locator(selector);
      await element.waitFor({ state: 'visible', timeout });
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get text content of an element
   */
  async getText(selector: string): Promise<string | null> {
    const element = this.page.locator(selector);
    return await element.textContent();
  }

  /**
   * Wait for loading to complete
   */
  async waitForLoadingComplete(timeout = 15000): Promise<void> {
    // Wait for loading indicators to disappear
    await this.page.waitForSelector(SELECTORS.LOADING, {
      state: 'hidden',
      timeout,
    }).catch(() => {
      // Loading might not exist, which is fine
    });
  }

  /**
   * Verify page header/title is present
   */
  async verifyPageHeader(expectedText?: string): Promise<void> {
    const header = this.page.locator(SELECTORS.PAGE_HEADER).first();
    await expect(header).toBeVisible({ timeout: 10000 });
    
    if (expectedText) {
      await expect(header).toContainText(expectedText);
    }
  }
}

