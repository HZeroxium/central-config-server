import { defineConfig, devices } from '@playwright/test';
import path from 'path';

/**
 * Playwright configuration specifically for recording tutorials
 * This config enables video, screenshots, and traces for all recordings
 */
export default defineConfig({
  testDir: './tests/recording',
  
  /* Run tests sequentially (not in parallel) for recording */
  fullyParallel: false,
  workers: 1,
  
  /* No retries for recording */
  retries: 0,
  
  /* Reporter to use */
  reporter: [
    ['html', { outputFolder: 'test-results/recordings/reports' }],
    ['list'],
  ],
  
  /* Shared settings for all recordings */
  use: {
    /* Base URL */
    baseURL: process.env.BASE_URL || 'http://localhost:3000',
    
    /* Always collect trace for recording */
    trace: 'on',
    
    /* Always take screenshots */
    screenshot: 'on',
    
    /* Always record video */
    video: 'on',
    
    /* Viewport size */
    viewport: { width: 1920, height: 1080 },
    
    /* Slower actions for better recording visibility */
    actionTimeout: 30000,
    navigationTimeout: 60000,
    
    /* Add delay between actions for better visibility in recordings */
    launchOptions: {
      slowMo: 100,
    },
  },
  
  /* Output directories */
  outputDir: 'test-results/recordings',
  
  /* Configure projects for different roles */
  projects: [
    {
      name: 'admin-recording',
      use: { 
        ...devices['Desktop Chrome'],
      },
      testMatch: /admin-tutorial\.ts$/,
    },
    {
      name: 'user-recording',
      use: { 
        ...devices['Desktop Chrome'],
      },
      testMatch: /user-tutorial\.ts$/,
    },
  ],
});

