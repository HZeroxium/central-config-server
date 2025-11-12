/**
 * Utility functions for parsing and formatting LEAF_LIST values.
 * 
 * LEAF_LIST stores a comma-separated list of elements in a single key.
 * Enhanced parsing rules:
 * - Separator: comma (,) by default, configurable
 * - Whitespace trimming: Yes (both sides) unless quoted
 * - Quoted strings: Support single (') and double (") quotes
 * - Escaped characters: Support escaped quotes and commas (\", \', \,)
 * - Empty string handling: Empty string → empty list []
 * - Empty elements: Filter out empty strings after trimming (e.g., "a,,b" → ["a", "b"])
 * - Multi-line values: Support newlines in quoted strings
 */

export interface ParseLeafListOptions {
  /** Custom separator (default: comma) */
  separator?: string;
  /** Whether to trim whitespace (default: true) */
  trim?: boolean;
  /** Whether to filter empty elements (default: true) */
  filterEmpty?: boolean;
  /** Whether to support quoted strings (default: true) */
  supportQuotes?: boolean;
}

export interface FormatLeafListOptions {
  /** Custom separator (default: comma) */
  separator?: string;
  /** Whether to quote elements containing separator or spaces (default: true) */
  autoQuote?: boolean;
  /** Whether to escape quotes and separators (default: true) */
  escape?: boolean;
}

/**
 * Parses a comma-separated string into a list of elements.
 * Enhanced with support for quoted strings and escaped characters.
 * 
 * @param value The comma-separated string value
 * @param options Parsing options
 * @returns Array of parsed elements
 */
export function parseLeafList(
  value: string,
  options: ParseLeafListOptions = {}
): string[] {
  if (!value || value.trim().length === 0) {
    return [];
  }

  const {
    separator = ",",
    trim = true,
    filterEmpty = true,
    supportQuotes = true,
  } = options;

  try {
    // If quotes are not supported, use simple parsing
    if (!supportQuotes) {
      const parts = value.split(separator);
      const elements: string[] = [];
      
      for (const part of parts) {
        const processed = trim ? part.trim() : part;
        if (!filterEmpty || processed.length > 0) {
          elements.push(processed);
        }
      }
      
      return elements;
    }

    // Advanced parsing with quoted string support
    const elements: string[] = [];
    let current = "";
    let inQuotes = false;
    let quoteChar: string | null = null;
    let i = 0;

    while (i < value.length) {
      const char = value[i];
      const nextChar = i + 1 < value.length ? value[i + 1] : null;

      if (!inQuotes) {
        // Not in quotes
        if (char === '"' || char === "'") {
          // Start of quoted string
          inQuotes = true;
          quoteChar = char;
          i++;
          continue;
        } else if (char === separator) {
          // Separator found - add current element
          const processed = trim ? current.trim() : current;
          if (!filterEmpty || processed.length > 0) {
            elements.push(processed);
          }
          current = "";
          i++;
          continue;
        } else if (char === "\\" && nextChar) {
          // Escaped character
          current += nextChar;
          i += 2;
          continue;
        } else {
          // Regular character
          current += char;
          i++;
        }
      } else {
        // In quotes
        if (char === quoteChar) {
          // End of quoted string
          if (nextChar === quoteChar) {
            // Escaped quote (double quote)
            current += quoteChar;
            i += 2;
          } else {
            // End of quoted string
            inQuotes = false;
            quoteChar = null;
            i++;
          }
        } else if (char === "\\" && nextChar) {
          // Escaped character
          current += nextChar;
          i += 2;
        } else {
          // Regular character (including newlines)
          current += char;
          i++;
        }
      }
    }

    // Add last element
    if (current.length > 0 || !filterEmpty) {
      const processed = trim ? current.trim() : current;
      if (!filterEmpty || processed.length > 0) {
        elements.push(processed);
      }
    }

    return elements;
  } catch (error) {
    // Graceful degradation: return empty list on parsing errors
    console.warn("Failed to parse LEAF_LIST value:", error);
    return [];
  }
}

/**
 * Formats a list of elements into a comma-separated string.
 * Enhanced with support for quoting and escaping.
 * 
 * @param elements Array of string elements
 * @param options Formatting options
 * @returns Comma-separated string (empty string if elements is empty)
 */
export function formatLeafList(
  elements: string[],
  options: FormatLeafListOptions = {}
): string {
  if (!elements || elements.length === 0) {
    return "";
  }

  const {
    separator = ",",
    autoQuote = true,
    escape = true,
  } = options;

  // Filter out empty/null elements
  const validElements = elements.filter(
    (el) => el != null && el.trim().length > 0
  );

  if (validElements.length === 0) {
    return "";
  }

  return validElements
    .map((el) => {
      const trimmed = el.trim();
      
      // Check if quoting is needed
      const needsQuoting =
        autoQuote &&
        (trimmed.includes(separator) ||
          trimmed.includes(" ") ||
          trimmed.includes("\n") ||
          trimmed.includes('"') ||
          trimmed.includes("'"));

      if (needsQuoting && escape) {
        // Escape quotes and backslashes
        let escaped = trimmed
          .replace(/\\/g, "\\\\")
          .replace(/"/g, '\\"');
        
        // Quote with double quotes
        return `"${escaped}"`;
      } else if (needsQuoting) {
        // Quote without escaping (use single quotes if double quotes are present)
        if (trimmed.includes('"') && !trimmed.includes("'")) {
          return `'${trimmed}'`;
        }
        return `"${trimmed}"`;
      } else {
        // No quoting needed
        return trimmed;
      }
    })
    .join(separator);
}

/**
 * Validates if a value can be parsed as a LEAF_LIST.
 * 
 * @param value The value to validate
 * @param options Parsing options
 * @returns True if valid, false otherwise
 */
export function isValidLeafList(
  value: string,
  options: ParseLeafListOptions = {}
): boolean {
  try {
    const parsed = parseLeafList(value, options);
    return Array.isArray(parsed);
  } catch {
    return false;
  }
}

/**
 * Detects duplicates in a LEAF_LIST.
 * 
 * @param elements Array of elements
 * @param caseSensitive Whether comparison should be case-sensitive (default: true)
 * @returns Array of duplicate elements (first occurrence kept)
 */
export function findDuplicates(
  elements: string[],
  caseSensitive: boolean = true
): string[] {
  const seen = new Set<string>();
  const duplicates: string[] = [];

  for (const element of elements) {
    const key = caseSensitive ? element : element.toLowerCase();
    if (seen.has(key)) {
      duplicates.push(element);
    } else {
      seen.add(key);
    }
  }

  return duplicates;
}

/**
 * Removes duplicates from a LEAF_LIST, keeping first occurrence.
 * 
 * @param elements Array of elements
 * @param caseSensitive Whether comparison should be case-sensitive (default: true)
 * @returns Array of unique elements
 */
export function removeDuplicates(
  elements: string[],
  caseSensitive: boolean = true
): string[] {
  const seen = new Set<string>();
  const unique: string[] = [];

  for (const element of elements) {
    const key = caseSensitive ? element : element.toLowerCase();
    if (!seen.has(key)) {
      seen.add(key);
      unique.push(element);
    }
  }

  return unique;
}

