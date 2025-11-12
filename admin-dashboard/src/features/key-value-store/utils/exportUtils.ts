/**
 * Export utility functions for KV Store entries
 * Supports JSON, YAML, Properties, and CSV formats
 */

import yaml from "js-yaml";

/**
 * Export data as JSON string
 * @param data - Data to export
 * @param metadata - Optional metadata to include
 * @returns JSON string
 */
export function exportAsJSON(
  data: unknown,
  metadata?: Record<string, unknown>
): string {
  const exportData = metadata
    ? {
        ...metadata,
        data,
      }
    : data;
  
  return JSON.stringify(exportData, null, 2);
}

/**
 * Export data as YAML string
 * @param data - Data to export
 * @param metadata - Optional metadata to include
 * @returns YAML string
 */
export function exportAsYAML(
  data: unknown,
  metadata?: Record<string, unknown>
): string {
  const exportData = metadata
    ? {
        ...metadata,
        data,
      }
    : data;
  
  try {
    return yaml.dump(exportData, {
      indent: 2,
      lineWidth: -1,
      quotingType: '"',
    });
  } catch (err) {
    // Fallback to JSON if YAML conversion fails
    return exportAsJSON(data, metadata);
  }
}

/**
 * Export data as Java Properties format
 * @param data - Data object (must be Record<string, unknown>)
 * @param metadata - Optional metadata to include
 * @returns Properties format string
 */
export function exportAsProperties(
  data: Record<string, unknown>,
  metadata?: Record<string, unknown>
): string {
  const lines: string[] = [];
  
  // Add metadata comments
  if (metadata) {
    lines.push("# Metadata");
    Object.entries(metadata).forEach(([key, value]) => {
      lines.push(`# ${key}=${String(value)}`);
    });
    lines.push("");
  }
  
  // Add data
  lines.push("# Data");
  const flattenObject = (
    obj: Record<string, unknown>,
    prefix = ""
  ): Record<string, string> => {
    const result: Record<string, string> = {};
    
    Object.entries(obj).forEach(([key, value]) => {
      const fullKey = prefix ? `${prefix}.${key}` : key;
      
      if (value === null || value === undefined) {
        result[fullKey] = "";
      } else if (typeof value === "object" && !Array.isArray(value)) {
        Object.assign(
          result,
          flattenObject(value as Record<string, unknown>, fullKey)
        );
      } else {
        result[fullKey] = String(value);
      }
    });
    
    return result;
  };
  
  const flattened = flattenObject(data);
  Object.entries(flattened).forEach(([key, value]) => {
    // Escape special characters in properties format
    const escapedKey = key.replace(/[=: ]/g, "\\$&");
    const escapedValue = String(value)
      .replace(/\\/g, "\\\\")
      .replace(/\n/g, "\\n")
      .replace(/\r/g, "\\r");
    lines.push(`${escapedKey}=${escapedValue}`);
  });
  
  return lines.join("\n");
}

/**
 * Export elements as CSV (one element per line)
 * @param elements - Array of string elements
 * @returns CSV string
 */
export function exportAsCSV(elements: string[]): string {
  if (!elements || elements.length === 0) return "";
  
  // Simple CSV: one element per line
  // If element contains comma, quote it
  return elements
    .map((element) => {
      if (element.includes(",") || element.includes('"') || element.includes("\n")) {
        // Escape quotes and wrap in quotes
        const escaped = element.replace(/"/g, '""');
        return `"${escaped}"`;
      }
      return element;
    })
    .join("\n");
}

/**
 * Download file with specified content
 * @param content - File content
 * @param filename - Filename
 * @param mimeType - MIME type
 */
export function downloadFile(
  content: string,
  filename: string,
  mimeType: string
): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

/**
 * Get MIME type for export format
 * @param format - Export format
 * @returns MIME type string
 */
export function getMimeType(format: "json" | "yaml" | "properties" | "csv"): string {
  switch (format) {
    case "json":
      return "application/json";
    case "yaml":
      return "application/x-yaml";
    case "properties":
      return "text/plain";
    case "csv":
      return "text/csv";
    default:
      return "text/plain";
  }
}

/**
 * Get file extension for export format
 * @param format - Export format
 * @returns File extension (without dot)
 */
export function getFileExtension(
  format: "json" | "yaml" | "properties" | "csv"
): string {
  switch (format) {
    case "json":
      return "json";
    case "yaml":
      return "yml";
    case "properties":
      return "properties";
    case "csv":
      return "csv";
    default:
      return "txt";
  }
}

/**
 * Generate filename for export
 * @param path - KV path
 * @param format - Export format
 * @param type - Entry type (LEAF, LIST, LEAF_LIST, FOLDER)
 * @returns Generated filename
 */
export function generateExportFilename(
  path: string,
  format: "json" | "yaml" | "properties" | "csv",
  type?: string
): string {
  const sanitizedPath = path.replace(/\//g, "-").replace(/[^a-zA-Z0-9-_]/g, "_");
  const extension = getFileExtension(format);
  const typePrefix = type ? `${type.toLowerCase()}-` : "";
  const timestamp = Date.now();
  
  return `${typePrefix}${sanitizedPath}-${timestamp}.${extension}`;
}

