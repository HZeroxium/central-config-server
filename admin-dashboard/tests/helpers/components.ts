import { Page, Locator, expect } from '@playwright/test';
import { SELECTORS } from '../constants/selectors';

/**
 * Component interaction helpers for common UI patterns
 */
export class ComponentHelper {
  constructor(private page: Page) {}

  /**
   * Table interaction helpers
   */
  async getTableRowCount(tableSelector: string = SELECTORS.TABLE): Promise<number> {
    const table = this.page.locator(tableSelector).first();
    const rows = table.locator(SELECTORS.TABLE_ROW);
    return await rows.count();
  }

  async clickTableRow(rowIndex: number, tableSelector: string = SELECTORS.TABLE): Promise<void> {
    const table = this.page.locator(tableSelector).first();
    const row = table.locator(SELECTORS.TABLE_ROW).nth(rowIndex);
    await row.click();
  }

  async getTableRowText(rowIndex: number, tableSelector: string = SELECTORS.TABLE): Promise<string | null> {
    const table = this.page.locator(tableSelector).first();
    const row = table.locator(SELECTORS.TABLE_ROW).nth(rowIndex);
    return await row.textContent();
  }

  async verifyTableHasData(tableSelector: string = SELECTORS.TABLE): Promise<void> {
    const rowCount = await this.getTableRowCount(tableSelector);
    expect(rowCount).toBeGreaterThan(0);
  }

  async verifyTableEmpty(tableSelector: string = SELECTORS.TABLE): Promise<void> {
    const emptyIndicator = this.page.locator(SELECTORS.EMPTY_TABLE).first();
    await expect(emptyIndicator).toBeVisible({ timeout: 5000 });
  }

  /**
   * Search interaction helpers
   */
  async typeInSearch(searchText: string, searchSelector: string = SELECTORS.SEARCH_INPUT): Promise<void> {
    const searchInput = this.page.locator(searchSelector).first();
    await searchInput.fill(searchText);
  }

  async submitSearch(searchButtonSelector: string = SELECTORS.SEARCH_BUTTON): Promise<void> {
    const searchButton = this.page.locator(searchButtonSelector).first();
    await searchButton.click();
  }

  async clearSearch(searchSelector: string = SELECTORS.SEARCH_INPUT): Promise<void> {
    const searchInput = this.page.locator(searchSelector).first();
    await searchInput.clear();
  }

  async verifySearchResults(expectedMinCount: number = 1): Promise<void> {
    // Wait for results to load
    await this.page.waitForTimeout(1000);
    
    // Check if table has data or empty state is shown
    const table = this.page.locator(SELECTORS.TABLE).first();
    const tableExists = await table.count() > 0;
    
    if (tableExists) {
      const rowCount = await this.getTableRowCount();
      expect(rowCount).toBeGreaterThanOrEqual(expectedMinCount);
    }
  }

  /**
   * Filter interaction helpers
   */
  async selectFilterOption(
    filterSelector: string,
    optionText: string
  ): Promise<void> {
    const filter = this.page.locator(filterSelector).first();
    await filter.click();
    
    // Wait for menu to appear
    await this.page.waitForSelector(SELECTORS.FILTER_MENU, { state: 'visible', timeout: 5000 });
    
    // Select option by text
    const option = this.page.locator(`${SELECTORS.FILTER_MENU} >> text="${optionText}"`).first();
    await option.click();
    
    // Wait for menu to close
    await this.page.waitForSelector(SELECTORS.FILTER_MENU, { state: 'hidden', timeout: 5000 });
  }

  async selectFilterByValue(
    filterSelector: string,
    value: string
  ): Promise<void> {
    const filter = this.page.locator(filterSelector).first();
    await filter.selectOption(value);
  }

  async clearFilter(filterSelector: string): Promise<void> {
    const filter = this.page.locator(filterSelector).first();
    await filter.selectOption({ index: 0 }); // Select first option (usually "All")
  }

  /**
   * Pagination interaction helpers
   */
  async clickNextPage(): Promise<void> {
    const nextButton = this.page.locator(SELECTORS.PAGINATION_NEXT).first();
    await nextButton.click();
    await this.page.waitForTimeout(1000); // Wait for page to load
  }

  async clickPreviousPage(): Promise<void> {
    const prevButton = this.page.locator(SELECTORS.PAGINATION_PREV).first();
    await prevButton.click();
    await this.page.waitForTimeout(1000); // Wait for page to load
  }

  async clickPageNumber(pageNumber: number): Promise<void> {
    const pageButton = this.page.locator(`${SELECTORS.PAGINATION_PAGE}:has-text("${pageNumber}")`).first();
    await pageButton.click();
    await this.page.waitForTimeout(1000); // Wait for page to load
  }

  async verifyPaginationExists(): Promise<boolean> {
    return await this.page.locator(SELECTORS.PAGINATION).count() > 0;
  }

  /**
   * Button interaction helpers
   */
  async clickButton(buttonText: string): Promise<void> {
    const button = this.page.locator(`button:has-text("${buttonText}")`).first();
    await button.click();
  }

  async clickButtonBySelector(selector: string): Promise<void> {
    const button = this.page.locator(selector).first();
    await button.click();
  }

  async verifyButtonEnabled(buttonText: string): Promise<void> {
    const button = this.page.locator(`button:has-text("${buttonText}")`).first();
    await expect(button).toBeEnabled();
  }

  async verifyButtonDisabled(buttonText: string): Promise<void> {
    const button = this.page.locator(`button:has-text("${buttonText}")`).first();
    await expect(button).toBeDisabled();
  }

  /**
   * Form interaction helpers
   */
  async fillTextField(selector: string, value: string): Promise<void> {
    const field = this.page.locator(selector).first();
    await field.fill(value);
  }

  async selectOption(selector: string, value: string): Promise<void> {
    const select = this.page.locator(selector).first();
    await select.selectOption(value);
  }

  async checkCheckbox(selector: string): Promise<void> {
    const checkbox = this.page.locator(selector).first();
    await checkbox.check();
  }

  async uncheckCheckbox(selector: string): Promise<void> {
    const checkbox = this.page.locator(selector).first();
    await checkbox.uncheck();
  }

  /**
   * Dialog/Drawer helpers
   */
  async waitForDialog(): Promise<Locator> {
    const dialog = this.page.locator(SELECTORS.DIALOG).first();
    await dialog.waitFor({ state: 'visible', timeout: 5000 });
    return dialog;
  }

  async closeDialog(): Promise<void> {
    const closeButton = this.page.locator(`${SELECTORS.DIALOG_ACTIONS} >> button:has-text("Cancel"), ${SELECTORS.DIALOG_ACTIONS} >> button[aria-label*="close" i]`).first();
    await closeButton.click();
    await this.page.waitForSelector(SELECTORS.DIALOG, { state: 'hidden', timeout: 5000 });
  }

  async waitForDrawer(): Promise<Locator> {
    const drawer = this.page.locator(SELECTORS.DRAWER).first();
    await drawer.waitFor({ state: 'visible', timeout: 5000 });
    return drawer;
  }

  async closeDrawer(): Promise<void> {
    // Try to find close button or click outside
    const closeButton = this.page.locator('button[aria-label*="close" i]').first();
    if (await closeButton.count() > 0) {
      await closeButton.click();
    } else {
      // Press Escape key
      await this.page.keyboard.press('Escape');
    }
    await this.page.waitForSelector(SELECTORS.DRAWER, { state: 'hidden', timeout: 5000 });
  }

  /**
   * Tab interaction helpers
   */
  async clickTab(tabText: string): Promise<void> {
    const tab = this.page.locator(`${SELECTORS.TAB}:has-text("${tabText}")`).first();
    await tab.click();
    await this.page.waitForTimeout(500); // Wait for tab content to load
  }

  async verifyTabActive(tabText: string): Promise<void> {
    const tab = this.page.locator(`${SELECTORS.TAB}:has-text("${tabText}")`).first();
    await expect(tab).toHaveAttribute('aria-selected', 'true');
  }

  /**
   * Alert/Toast helpers
   */
  async waitForAlert(): Promise<Locator> {
    const alert = this.page.locator(SELECTORS.ALERT).first();
    await alert.waitFor({ state: 'visible', timeout: 5000 });
    return alert;
  }

  async verifySuccessMessage(message?: string): Promise<void> {
    const alert = await this.waitForAlert();
    await expect(alert.locator(SELECTORS.ALERT_SUCCESS)).toBeVisible();
    if (message) {
      await expect(alert).toContainText(message);
    }
  }

  async verifyErrorMessage(message?: string): Promise<void> {
    const alert = await this.waitForAlert();
    await expect(alert.locator(SELECTORS.ALERT_ERROR)).toBeVisible();
    if (message) {
      await expect(alert).toContainText(message);
    }
  }
}

