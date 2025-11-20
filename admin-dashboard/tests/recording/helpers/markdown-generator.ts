/// <reference types="node" />
import * as fs from 'node:fs';
import * as path from 'node:path';
import type { RecordingSession, RecordingModule } from '../types';
import { MODULE_METADATA, MODULE_GROUPS, TUTORIALS_DIR, RECORDING_OUTPUT_DIR } from '../constants';

/**
 * Generate markdown documentation from recording session
 */
export class MarkdownGenerator {
  /**
   * Generate markdown for a recording session
   */
  async generateMarkdown(session: RecordingSession): Promise<string> {
    const { role, modules, metadata, startTime, endTime } = session;
    const duration = endTime ? Math.round((endTime - startTime) / 1000) : 0;
    
    let markdown = `# ${role === 'admin' ? 'Admin' : 'User'} Tutorial\n\n`;
    markdown += `**Generated:** ${new Date(startTime).toLocaleString()}\n`;
    markdown += `**Duration:** ${duration} seconds\n`;
    markdown += `**Browser:** ${metadata.browser}\n`;
    markdown += `**Viewport:** ${metadata.viewport.width}x${metadata.viewport.height}\n\n`;
    
    markdown += `---\n\n`;
    
    // Table of Contents
    markdown += `## Table of Contents\n\n`;
    for (const group of MODULE_GROUPS) {
      const groupModules = modules.filter(m => group.modules.includes(m.name));
      if (groupModules.length > 0) {
        markdown += `- [${group.label}](#${group.name})\n`;
        for (const module of groupModules) {
          markdown += `  - [${module.label}](#${module.name})\n`;
        }
      }
    }
    markdown += `\n---\n\n`;
    
    // Prerequisites
    markdown += `## Prerequisites\n\n`;
    markdown += `- Access to the admin dashboard at \`${metadata.baseUrl}\`\n`;
    markdown += `- Valid ${role === 'admin' ? 'admin' : 'user'} credentials\n`;
    markdown += `- Browser: Chrome (recommended)\n\n`;
    markdown += `---\n\n`;
    
    // Modules by group
    for (const group of MODULE_GROUPS) {
      const groupModules = modules.filter(m => group.modules.includes(m.name));
      if (groupModules.length === 0) continue;
      
      markdown += `## ${group.label}\n\n`;
      
      for (const module of groupModules) {
        markdown += this.generateModuleSection(module, role);
      }
    }
    
    // Tips & Notes
    markdown += `---\n\n`;
    markdown += `## Tips & Notes\n\n`;
    markdown += `- Use the sidebar navigation to quickly access different modules\n`;
    markdown += `- Most list pages support search and filtering\n`;
    markdown += `- Click on table rows to view detail pages\n`;
    if (role === 'admin') {
      markdown += `- Admin users have access to IAM management features\n`;
    } else {
      markdown += `- Regular users cannot access IAM management features\n`;
    }
    markdown += `\n`;
    
    return markdown;
  }

  /**
   * Generate markdown section for a module
   */
  private generateModuleSection(module: RecordingModule, role: 'admin' | 'user'): string {
    const metadata = MODULE_METADATA[module.name];
    let markdown = `### ${metadata?.label || module.label}\n\n`;
    
    if (metadata?.description) {
      markdown += `${metadata.description}\n\n`;
    }
    
    markdown += `**Route:** \`${metadata?.route || module.route}\`\n\n`;
    
    if (module.steps.length > 0) {
      markdown += `#### Steps\n\n`;
      
      for (const step of module.steps) {
        markdown += `**Step ${step.stepNumber}: ${step.name}**\n\n`;
        markdown += `${step.description}\n\n`;
        
        if (step.interactions && step.interactions.length > 0) {
          markdown += `*Interactions:* ${step.interactions.join(', ')}\n\n`;
        }
        
        if (step.screenshotPath) {
          // Use relative path from tutorials directory
          const relativePath = path.relative(
            path.join(RECORDING_OUTPUT_DIR, TUTORIALS_DIR),
            step.screenshotPath
          );
          markdown += `![${step.name}](${relativePath})\n\n`;
        }
        
        markdown += `---\n\n`;
      }
    }
    
    return markdown;
  }

  /**
   * Save markdown to file
   */
  async saveMarkdown(content: string, filename: string, role: 'admin' | 'user'): Promise<string> {
    const tutorialsDir = path.join(RECORDING_OUTPUT_DIR, TUTORIALS_DIR);
    await fs.promises.mkdir(tutorialsDir, { recursive: true });
    
    const filePath = path.join(tutorialsDir, filename);
    await fs.promises.writeFile(filePath, content, 'utf-8');
    
    return filePath;
  }

  /**
   * Generate and save tutorial markdown
   */
  async generateAndSave(session: RecordingSession): Promise<string> {
    const markdown = await this.generateMarkdown(session);
    const filename = `${session.role}-tutorial.md`;
    return await this.saveMarkdown(markdown, filename, session.role);
  }
}

