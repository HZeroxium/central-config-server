import { Page } from '@playwright/test';
import { BaseDetailPage } from './BaseDetailPage';

/**
 * Approval Detail Page Object
 */
export class ApprovalDetailPage extends BaseDetailPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to approval detail
   */
  async gotoDetail(approvalId: string): Promise<void> {
    await this.goto(`/approvals/${approvalId}`);
    await this.waitForDetailLoad();
  }
}

