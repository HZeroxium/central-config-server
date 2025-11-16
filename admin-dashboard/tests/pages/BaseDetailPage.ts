import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { SELECTORS } from '../constants/selectors';

/**
 * Base class for detail pages with common patterns (tabs, cards, sections)
 */
export abstract class BaseDetailPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Wait for detail page to load
   */
  async waitForDetailLoad(): Promise<void> {
    await this.waitForPageLoad();
    await this.waitForLoadingComplete();
  }

  /**
   * Verify detail page is loaded
   */
  async verifyDetailLoaded(): Promise<void> {
    await this.verifyPageLoaded();
    // Detail pages should have at least one card or section
    const cards = this.page.locator(SELECTORS.CARD);
    const cardCount = await cards.count();
    expect(cardCount).toBeGreaterThan(0);
  }

  /**
   * Click tab by text
   */
  async clickTab(tabText: string): Promise<void> {
    const tab = this.page.locator(`${SELECTORS.TAB}:has-text("${tabText}")`).first();
    await tab.click();
    await this.page.waitForTimeout(500); // Wait for tab content to load
  }

  /**
   * Verify tab is active
   */
  async verifyTabActive(tabText: string): Promise<void> {
    const tab = this.page.locator(`${SELECTORS.TAB}:has-text("${tabText}")`).first();
    await expect(tab).toHaveAttribute('aria-selected', 'true');
  }

  /**
   * Get card by heading text
   */
  getCardByHeading(heading: string): Locator {
    return this.page.locator(`.MuiCard-root:has-text("${heading}")`).first();
  }

  /**
   * Verify card is visible
   */
  async verifyCardVisible(heading: string): Promise<void> {
    const card = this.getCardByHeading(heading);
    await expect(card).toBeVisible({ timeout: 10000 });
  }

  /**
   * Navigate back
   */
  async navigateBack(): Promise<void> {
    await this.page.goBack();
    await this.waitForNavigation();
  }

  /**
   * Click back button if present
   */
  async clickBackButton(): Promise<void> {
    const backButton = this.page.locator('button:has-text("Back"), button[aria-label*="back" i]').first();
    const exists = await backButton.count() > 0;
    if (exists) {
      await backButton.click();
      await this.waitForNavigation();
    }
  }
}

