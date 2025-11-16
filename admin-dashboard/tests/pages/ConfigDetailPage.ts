import { Page } from '@playwright/test';
import { BaseDetailPage } from './BaseDetailPage';

/**
 * Config Detail Page Object
 */
export class ConfigDetailPage extends BaseDetailPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to config detail
   */
  async gotoDetail(application: string, profile: string, label?: string): Promise<void> {
    let path = `/configs/${encodeURIComponent(application)}/${encodeURIComponent(profile)}`;
    if (label) {
      path += `?label=${encodeURIComponent(label)}`;
    }
    await this.goto(path);
    await this.waitForDetailLoad();
  }
}

