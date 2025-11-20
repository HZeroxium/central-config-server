/// <reference types="node" />
import { Page } from '@playwright/test';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { getScreenshotsPath, RECORDING_DELAYS } from '../constants';
import type { RecordingStep } from '../types';

/**
 * Helper utilities for recording sessions
 */
export class RecordingHelper {
  private steps: RecordingStep[] = [];
  private currentModule: string = '';
  private role: 'admin' | 'user';

  constructor(private page: Page, role: 'admin' | 'user') {
    this.role = role;
  }

  /**
   * Set current module context
   */
  setModule(moduleName: string): void {
    this.currentModule = moduleName;
  }

  /**
   * Wait for page to be ready with recording delay
   */
  async waitForPageReady(timeout = 30000): Promise<void> {
    await this.page.waitForLoadState('networkidle', { timeout });
    await this.page.waitForTimeout(RECORDING_DELAYS.PAGE_LOAD);
  }

  /**
   * Take screenshot with naming convention
   */
  async takeScreenshot(stepName: string, moduleName?: string): Promise<string> {
    const module = moduleName || this.currentModule;
    const screenshotsDir = getScreenshotsPath(this.role, module);
    
    // Ensure directory exists
    await fs.promises.mkdir(screenshotsDir, { recursive: true });
    
    // Generate filename
    const stepNumber = this.steps.length + 1;
    const timestamp = Date.now();
    const filename = `step-${String(stepNumber).padStart(2, '0')}-${stepName}-${timestamp}.png`;
    const filePath = path.join(screenshotsDir, filename);
    
    // Take screenshot
    await this.page.screenshot({
      path: filePath,
      fullPage: true,
    });
    
    await this.page.waitForTimeout(RECORDING_DELAYS.SCREENSHOT);
    
    return filePath;
  }

  /**
   * Record a step with metadata
   */
  recordStep(
    name: string,
    description: string,
    route: string,
    interactions?: string[]
  ): RecordingStep {
    const step: RecordingStep = {
      stepNumber: this.steps.length + 1,
      name,
      description,
      module: this.currentModule,
      route,
      interactions,
      timestamp: Date.now(),
    };
    
    this.steps.push(step);
    return step;
  }

  /**
   * Record step and take screenshot
   */
  async recordStepWithScreenshot(
    name: string,
    description: string,
    route: string,
    interactions?: string[]
  ): Promise<RecordingStep> {
    const step = this.recordStep(name, description, route, interactions);
    const screenshotPath = await this.takeScreenshot(name);
    step.screenshotPath = screenshotPath;
    return step;
  }

  /**
   * Get all recorded steps
   */
  getSteps(): RecordingStep[] {
    return this.steps;
  }

  /**
   * Clear recorded steps
   */
  clearSteps(): void {
    this.steps = [];
  }

  /**
   * Wait for interaction delay
   */
  async waitForInteraction(): Promise<void> {
    await this.page.waitForTimeout(RECORDING_DELAYS.INTERACTION);
  }

  /**
   * Wait for navigation delay
   */
  async waitForNavigation(): Promise<void> {
    await this.page.waitForTimeout(RECORDING_DELAYS.NAVIGATION);
  }
}

