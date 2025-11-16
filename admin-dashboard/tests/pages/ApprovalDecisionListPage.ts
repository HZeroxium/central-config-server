import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * Approval Decision List Page Object
 */
export class ApprovalDecisionListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to approval decisions list
   */
  async gotoList(): Promise<void> {
    await this.goto('/approval-decisions');
    await this.waitForListLoad();
  }
}

