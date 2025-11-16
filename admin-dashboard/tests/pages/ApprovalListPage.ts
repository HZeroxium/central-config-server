import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * Approval List Page Object
 */
export class ApprovalListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to approvals list
   */
  async goto(): Promise<void> {
    await this.goto('/approvals');
    await this.waitForListLoad();
  }

  /**
   * Filter by status
   */
  async filterByStatus(status: string): Promise<void> {
    await this.interactWithFilter('Status', status);
  }

  /**
   * Filter by request type
   */
  async filterByRequestType(type: string): Promise<void> {
    await this.interactWithFilter('Request Type', type);
  }
}

