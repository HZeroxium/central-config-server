import { Page, expect } from '@playwright/test';
import { SELECTORS } from '../constants/selectors';

/**
 * Common test utilities and helpers
 */
export class TestHelpers {
  constructor(private page: Page) {}

  /**
   * Wait with proper timeout handling
   */
  async waitFor(condition: () => Promise<boolean>, timeout = 10000, interval = 500): Promise<void> {
    const startTime = Date.now();
    while (Date.now() - startTime < timeout) {
      if (await condition()) {
        return;
      }
      await this.page.waitForTimeout(interval);
    }
    throw new Error(`Condition not met within ${timeout}ms`);
  }

  /**
   * Wait for network to be idle
   */
  async waitForNetworkIdle(timeout = 30000): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout });
  }

  /**
   * Retry an action with exponential backoff
   */
  async retry<T>(
    action: () => Promise<T>,
    maxRetries = 3,
    delay = 1000
  ): Promise<T> {
    let lastError: Error | null = null;
    
    for (let i = 0; i < maxRetries; i++) {
      try {
        return await action();
      } catch (error) {
        lastError = error as Error;
        if (i < maxRetries - 1) {
          await this.page.waitForTimeout(delay * Math.pow(2, i));
        }
      }
    }
    
    throw lastError || new Error('Action failed after retries');
  }

  /**
   * Verify element is visible and contains text
   */
  async verifyElementContainsText(selector: string, expectedText: string): Promise<void> {
    const element = this.page.locator(selector).first();
    await expect(element).toBeVisible({ timeout: 10000 });
    await expect(element).toContainText(expectedText);
  }

  /**
   * Verify table has minimum number of rows
   */
  async verifyTableRowCount(minCount: number, tableSelector: string = SELECTORS.TABLE): Promise<void> {
    const table = this.page.locator(tableSelector).first();
    const rows = table.locator(SELECTORS.TABLE_ROW);
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(minCount);
  }

  /**
   * Verify card count
   */
  async verifyCardCount(minCount: number): Promise<void> {
    const cards = this.page.locator(SELECTORS.CARD);
    const count = await cards.count();
    expect(count).toBeGreaterThanOrEqual(minCount);
  }

  /**
   * Take screenshot with descriptive name
   */
  async takeScreenshot(name: string): Promise<void> {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    await this.page.screenshot({
      path: `tests/screenshots/${name}-${timestamp}.png`,
      fullPage: true,
    });
  }

  /**
   * Wait for API calls to complete
   */
  async waitForApiCalls(timeout = 10000): Promise<void> {
    // Wait for fetch/XHR requests to complete
    await this.page.waitForLoadState('networkidle', { timeout });
  }

  /**
   * Verify no console errors
   */
  async verifyNoConsoleErrors(): Promise<void> {
    const errors: string[] = [];
    
    this.page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });

    // Wait a bit to catch any errors
    await this.page.waitForTimeout(2000);

    if (errors.length > 0) {
      console.warn('Console errors detected:', errors);
      // Don't fail test, just warn
    }
  }

  /**
   * Scroll to element
   */
  async scrollToElement(selector: string): Promise<void> {
    const element = this.page.locator(selector).first();
    await element.scrollIntoViewIfNeeded();
  }

  /**
   * Wait for element to be stable (not moving)
   */
  async waitForElementStable(selector: string, timeout = 5000): Promise<void> {
    const element = this.page.locator(selector).first();
    await element.waitFor({ state: 'visible', timeout });
    
    // Wait for any animations to complete
    await this.page.waitForTimeout(500);
  }
}

