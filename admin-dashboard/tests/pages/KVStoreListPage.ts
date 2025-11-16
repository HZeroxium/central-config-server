import { Page } from '@playwright/test';
import { BaseListPage } from './BaseListPage';

/**
 * KV Store List Page Object
 */
export class KVStoreListPage extends BaseListPage {
  constructor(page: Page) {
    super(page);
  }

  /**
   * Navigate to KV store list
   */
  async goto(): Promise<void> {
    await this.goto('/kv');
    await this.waitForListLoad();
  }
}

