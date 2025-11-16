import { Page } from '@playwright/test';
import { BaseDetailPage } from './BaseDetailPage';

/**
 * Service Instance Detail Page Object
 */
export class ServiceInstanceDetailPage extends BaseDetailPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to service instance detail
   */
  async gotoDetail(serviceName: string, instanceId: string): Promise<void> {
    await this.goto(`/service-instances/${serviceName}/${instanceId}`);
    await this.waitForDetailLoad();
  }
}

