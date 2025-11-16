import { Page } from '@playwright/test';
import { BasePage } from './BasePage';
import { SELECTORS } from '../constants/selectors';

/**
 * Service Registry List Page Object
 */
export class ServiceRegistryListPage extends BasePage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to service registry list
   */
  async goto(): Promise<void> {
    await this.goto('/registry');
    await this.waitForPageLoad();
    await this.waitForLoadingComplete();
  }

  /**
   * Search by service name
   */
  async searchService(searchText: string): Promise<void> {
    const searchInput = this.page.locator(SELECTORS.SEARCH_INPUT).first();
    await searchInput.fill(searchText);
    await this.page.waitForTimeout(1000); // Wait for debounce
  }

  /**
   * Filter by tag
   */
  async filterByTag(tag: string): Promise<void> {
    const filterSelect = this.page.locator('select, .MuiSelect-root').first();
    await filterSelect.selectOption(tag);
    await this.waitForLoadingComplete();
  }
}

