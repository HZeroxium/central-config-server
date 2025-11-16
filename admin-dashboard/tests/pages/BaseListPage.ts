import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './BasePage';
import { SELECTORS } from '../constants/selectors';
import { ComponentHelper } from '../helpers/components';

/**
 * Base class for list pages with common patterns (tables, search, filters, pagination)
 */
export abstract class BaseListPage extends BasePage {
  public componentHelper: ComponentHelper;

  constructor(page: Page) {
    super(page);
    this.componentHelper = new ComponentHelper(page);
  }

  /**
   * Wait for list page to load with data
   */
  async waitForListLoad(): Promise<void> {
    await this.waitForPageLoad();
    await this.waitForLoadingComplete();
    
    // Wait for table or data grid to appear
    const table = this.page.locator(SELECTORS.TABLE).first();
    await table.waitFor({ state: 'visible', timeout: 15000 });
  }

  /**
   * Verify list page is loaded with data
   */
  async verifyListLoaded(): Promise<void> {
    await this.verifyPageLoaded();
    await this.verifyDataPresent();
  }

  /**
   * Get table/data grid locator
   */
  getTable(): Locator {
    return this.page.locator(SELECTORS.TABLE).first();
  }

  /**
   * Get search input locator
   */
  getSearchInput(): Locator {
    return this.page.locator(SELECTORS.SEARCH_INPUT).first();
  }

  /**
   * Interact with search
   */
  async interactWithSearch(searchText: string): Promise<void> {
    const searchInput = this.getSearchInput();
    await searchInput.fill(searchText);
    await this.page.keyboard.press('Enter');
    await this.waitForLoadingComplete();
  }

  /**
   * Interact with filter dropdown
   */
  async interactWithFilter(filterLabel: string, optionText: string): Promise<void> {
    // Find filter by label
    const filterLabelElement = this.page.locator(`label:has-text("${filterLabel}")`).first();
    const filterSelect = filterLabelElement.locator('..').locator(SELECTORS.SELECT).first();
    
    await filterSelect.click();
    await this.page.waitForTimeout(500);
    
    // Select option
    const option = this.page.locator(`text="${optionText}"`).first();
    await option.click();
    
    await this.waitForLoadingComplete();
  }

  /**
   * Interact with pagination
   */
  async interactWithPagination(action: 'next' | 'prev' | 'page', pageNumber?: number): Promise<void> {
    const pagination = this.page.locator(SELECTORS.PAGINATION).first();
    const exists = await pagination.count() > 0;
    
    if (!exists) {
      return; // No pagination available
    }

    if (action === 'next') {
      await this.componentHelper.clickNextPage();
    } else if (action === 'prev') {
      await this.componentHelper.clickPreviousPage();
    } else if (action === 'page' && pageNumber) {
      await this.componentHelper.clickPageNumber(pageNumber);
    }
    
    await this.waitForLoadingComplete();
  }

  /**
   * Navigate to detail page by clicking row
   */
  async navigateToDetail(rowIndex: number = 0): Promise<void> {
    const table = this.getTable();
    const row = table.locator(SELECTORS.TABLE_ROW).nth(rowIndex);
    await row.click();
    await this.waitForNavigation();
  }

  /**
   * Verify table has data
   */
  async verifyTableHasData(): Promise<void> {
    await this.componentHelper.verifyTableHasData();
  }

  /**
   * Get row count
   */
  async getRowCount(): Promise<number> {
    return await this.componentHelper.getTableRowCount();
  }

  /**
   * Click refresh button
   */
  async clickRefresh(): Promise<void> {
    const refreshButton = this.page.locator(SELECTORS.REFRESH_BUTTON).first();
    await refreshButton.click();
    await this.waitForLoadingComplete();
  }
}

