/**
 * File handling utilities for SDK integration wizard.
 */

/**
 * Reads file content as text.
 */
export async function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      if (e.target?.result) {
        resolve(e.target.result as string);
      } else {
        reject(new Error('Failed to read file'));
      }
    };
    reader.onerror = () => reject(new Error('File read error'));
    reader.readAsText(file);
  });
}

/**
 * Detects configuration format from file name or content.
 */
export function detectConfigFormat(fileName: string, content: string): 'yaml' | 'properties' | 'ini' {
  const lowerName = fileName.toLowerCase();
  
  if (lowerName.endsWith('.ini')) {
    return 'ini';
  }
  if (lowerName.endsWith('.properties') || lowerName.endsWith('.props')) {
    return 'properties';
  }
  if (lowerName.endsWith('.yml') || lowerName.endsWith('.yaml')) {
    return 'yaml';
  }
  
  // Try to detect from content
  if (content.includes('[') && content.includes(']') && content.includes('=')) {
    return 'ini';
  }
  if (content.includes('=') && !content.includes(':') && !content.includes('-')) {
    return 'properties';
  }
  
  return 'yaml'; // Default
}

/**
 * Downloads content as a file.
 */
export function downloadFile(content: string, fileName: string, mimeType: string = 'text/plain'): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

/**
 * Copies text to clipboard.
 */
export async function copyToClipboard(text: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text);
  } catch (err) {
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.opacity = '0';
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand('copy');
    document.body.removeChild(textArea);
  }
}

/**
 * Formats markdown checklist from array of items.
 */
export function formatChecklist(items: string[]): string {
  return items.map(item => `- [ ] ${item}`).join('\n');
}

