/// <reference types="node" />
import * as fs from 'node:fs';
import * as path from 'node:path';
import { RECORDING_OUTPUT_DIR, getModuleOutputPath, SCREENSHOTS_DIR } from '../constants';

/**
 * Organize output files by module structure
 */
export class OutputOrganizer {
  /**
   * Organize output files for a role
   */
  async organizeByModule(role: 'admin' | 'user'): Promise<void> {
    const roleDir = path.join(RECORDING_OUTPUT_DIR, role);
    
    if (!fs.existsSync(roleDir)) {
      return;
    }
    
    // Find all module directories
    const entries = await fs.promises.readdir(roleDir, { withFileTypes: true });
    
    for (const entry of entries) {
      if (entry.isDirectory()) {
        const moduleDir = path.join(roleDir, entry.name);
        await this.organizeModuleDirectory(moduleDir, entry.name);
      }
    }
  }

  /**
   * Organize files within a module directory
   */
  private async organizeModuleDirectory(moduleDir: string, moduleName: string): Promise<void> {
    const entries = await fs.promises.readdir(moduleDir, { withFileTypes: true });
    
    // Ensure screenshots directory exists
    const screenshotsDir = path.join(moduleDir, SCREENSHOTS_DIR);
    await fs.promises.mkdir(screenshotsDir, { recursive: true });
    
    // Move screenshots to screenshots directory
    for (const entry of entries) {
      if (entry.isFile() && entry.name.endsWith('.png')) {
        const oldPath = path.join(moduleDir, entry.name);
        const newPath = path.join(screenshotsDir, entry.name);
        
        // Only move if not already in screenshots directory
        if (!oldPath.startsWith(screenshotsDir)) {
          await fs.promises.rename(oldPath, newPath);
        }
      }
    }
  }

  /**
   * Create directory structure for a module
   */
  async createModuleStructure(role: 'admin' | 'user', moduleName: string): Promise<string> {
    const modulePath = getModuleOutputPath(role, moduleName);
    const screenshotsPath = path.join(modulePath, SCREENSHOTS_DIR);
    
    await fs.promises.mkdir(screenshotsPath, { recursive: true });
    
    return modulePath;
  }

  /**
   * Clean up old recordings (optional)
   */
  async cleanupOldRecordings(role: 'admin' | 'user', daysOld = 7): Promise<void> {
    const roleDir = path.join(RECORDING_OUTPUT_DIR, role);
    
    if (!fs.existsSync(roleDir)) {
      return;
    }
    
    const entries = await fs.promises.readdir(roleDir, { withFileTypes: true });
    const cutoffTime = Date.now() - daysOld * 24 * 60 * 60 * 1000;
    
    for (const entry of entries) {
      const entryPath = path.join(roleDir, entry.name);
      const stats = await fs.promises.stat(entryPath);
      
      if (stats.mtimeMs < cutoffTime) {
        await fs.promises.rm(entryPath, { recursive: true, force: true });
      }
    }
  }
}

