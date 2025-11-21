/**
 * Code formatting utilities for SDK integration wizard.
 */

/**
 * Formats Java code with basic indentation.
 */
export function formatJavaCode(code: string): string {
  // Basic Java formatting - in a real implementation, you might use a proper formatter
  const lines = code.split('\n');
  let indentLevel = 0;
  const indentSize = 4;
  
  return lines.map(line => {
    const trimmed = line.trim();
    if (trimmed === '') {
      return '';
    }
    
    // Decrease indent before closing braces
    if (trimmed.startsWith('}') || trimmed.startsWith(']') || trimmed.startsWith(')')) {
      indentLevel = Math.max(0, indentLevel - 1);
    }
    
    const indented = ' '.repeat(indentLevel * indentSize) + trimmed;
    
    // Increase indent after opening braces
    if (trimmed.endsWith('{') || trimmed.endsWith('[') || trimmed.endsWith('(')) {
      indentLevel++;
    }
    
    return indented;
  }).join('\n');
}

/**
 * Formats YAML code (basic - YAML is usually already formatted).
 */
export function formatYamlCode(yaml: string): string {
  // YAML is usually already formatted, but we can do basic cleanup
  return yaml
    .split('\n')
    .map(line => line.trimEnd())
    .join('\n');
}

/**
 * Formats markdown checklist.
 */
export function formatChecklist(items: string[]): string {
  return items.map(item => `- [ ] ${item}`).join('\n');
}

