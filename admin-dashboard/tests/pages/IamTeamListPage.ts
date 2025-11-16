import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * IAM Team List Page Object (Admin only)
 */
export class IamTeamListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to IAM teams list
   */
  async goto(): Promise<void> {
    await this.goto('/iam/teams');
    await this.waitForListLoad();
  }
}

