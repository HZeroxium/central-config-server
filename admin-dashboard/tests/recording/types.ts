/**
 * TypeScript types for recording metadata
 */

export interface RecordingStep {
  stepNumber: number;
  name: string;
  description: string;
  module: string;
  route: string;
  screenshotPath?: string;
  timestamp: number;
  interactions?: string[];
}

export interface RecordingModule {
  name: string;
  label: string;
  route: string;
  description: string;
  steps: RecordingStep[];
  screenshots: string[];
  videoPath?: string;
  tracePath?: string;
}

export interface RecordingSession {
  role: 'admin' | 'user';
  startTime: number;
  endTime?: number;
  modules: RecordingModule[];
  metadata: {
    baseUrl: string;
    viewport: { width: number; height: number };
    browser: string;
  };
}

export interface ModuleGroup {
  name: string;
  label: string;
  modules: string[];
}

