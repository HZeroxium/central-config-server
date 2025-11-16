import { Page, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { SELECTORS } from '../constants/selectors';

/**
 * Config List Page Object
 */
export class ConfigListPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to configs list
   */
  async goto(): Promise<void> {
    await this.goto('/configs');
    await this.waitForPageLoad();
  }

  /**
   * Fill application name
   */
  async fillApplication(application: string): Promise<void> {
    const appInput = this.page.locator('input[placeholder*="application" i], input[label*="Application" i]').first();
    await appInput.fill(application);
  }

  /**
   * Fill profile
   */
  async fillProfile(profile: string): Promise<void> {
    const profileInput = this.page.locator('input[placeholder*="profile" i], input[label*="Profile" i]').first();
    await profileInput.fill(profile);
  }

  /**
   * Fill label (optional)
   */
  async fillLabel(label: string): Promise<void> {
    const labelInput = this.page.locator('input[placeholder*="label" i], input[label*="Label" i]').first();
    await labelInput.fill(label);
  }

  /**
   * Click search/submit button
   */
  async clickSearch(): Promise<void> {
    const searchButton = this.page.locator('button:has-text("Search"), button[type="submit"]').first();
    await searchButton.click();
    await this.waitForNavigation();
  }

  /**
   * Search for config
   */
  async searchConfig(application: string, profile: string, label?: string): Promise<void> {
    await this.fillApplication(application);
    await this.fillProfile(profile);
    if (label) {
      await this.fillLabel(label);
    }
    await this.clickSearch();
  }

  /**
   * Verify config search form is present
   */
  async verifySearchFormPresent(): Promise<void> {
    const form = this.page.locator('form, .MuiCard-root').first();
    await expect(form).toBeVisible();
  }
}

