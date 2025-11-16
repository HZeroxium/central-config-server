import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * Drift Event List Page Object
 */
export class DriftEventListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to drift events list
   */
  async goto(): Promise<void> {
    await this.goto('/drift-events');
    await this.waitForListLoad();
  }

  /**
   * Filter by status
   */
  async filterByStatus(status: 'DETECTED' | 'RESOLVED' | 'IGNORED'): Promise<void> {
    await this.interactWithFilter('Status', status);
  }

  /**
   * Filter by severity
   */
  async filterBySeverity(severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'): Promise<void> {
    await this.interactWithFilter('Severity', severity);
  }

  /**
   * Toggle unresolved only
   */
  async toggleUnresolvedOnly(): Promise<void> {
    const switchElement = this.page.locator(SELECTORS.SWITCH).first();
    await switchElement.click();
    await this.waitForLoadingComplete();
  }
}

