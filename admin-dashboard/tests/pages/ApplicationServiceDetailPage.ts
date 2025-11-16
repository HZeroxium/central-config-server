import { Page } from '@playwright/test';
import { BaseDetailPage } from './BaseDetailPage';

/**
 * Application Service Detail Page Object
 */
export class ApplicationServiceDetailPage extends BaseDetailPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to application service detail
   */
  async gotoDetail(serviceId: string): Promise<void> {
    await this.goto(`/application-services/${serviceId}`);
    await this.waitForDetailLoad();
  }

  /**
   * Click on tabs
   */
  async clickInstancesTab(): Promise<void> {
    await this.clickTab('Instances');
  }

  async clickSharesTab(): Promise<void> {
    await this.clickTab('Shares');
  }

  async clickApprovalsTab(): Promise<void> {
    await this.clickTab('Approvals');
  }
}

