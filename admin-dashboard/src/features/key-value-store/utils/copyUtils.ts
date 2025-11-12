/**
 * Copy utility functions for KV Store entries
 * Supports context-based separators and curl command generation
 */

import yaml from "js-yaml";

/**
 * Copy path with specified separator
 * @param path - Full path (e.g., "config/database/url")
 * @param separator - Separator to use: '/' for paths, '.' for nested configs
 * @returns Formatted path string
 */
export function copyPath(path: string, separator: "/" | "." = "/"): string {
  if (!path) return "";
  
  if (separator === "/") {
    return path;
  } else {
    // Convert / to . for nested config format
    return path.replace(/\//g, ".");
  }
}

/**
 * Copy value with optional formatting
 * @param value - Raw value string
 * @param format - Format type: 'json' for JSON, 'yaml' for YAML, undefined for raw
 * @returns Formatted value string
 */
export function copyValue(value: string, format?: "json" | "yaml"): string {
  if (!value) return "";
  
  if (format === "json") {
    try {
      const parsed = JSON.parse(value);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return value;
    }
  } else if (format === "yaml") {
    try {
      const parsed = JSON.parse(value);
      return yaml.dump(parsed, { indent: 2 });
    } catch {
      return value;
    }
  }
  
  return value;
}

/**
 * Copy LEAF_LIST elements with specified separator
 * @param elements - Array of elements
 * @param separator - Separator to use (default: ',')
 * @returns Formatted string
 */
export function copyLeafListElements(
  elements: string[],
  separator: "," = ","
): string {
  if (!elements || elements.length === 0) return "";
  return elements.join(separator);
}

/**
 * Generate curl command for KV operations
 * @param serviceId - Service ID
 * @param path - KV path
 * @param method - HTTP method
 * @param data - Request body data (for PUT)
 * @param baseUrl - Base URL (default: window.location.origin)
 * @param outputFormat - Output format: 'json' or 'raw'
 * @returns curl command string
 */
export function copyAsCurl(
  serviceId: string,
  path: string,
  method: "GET" | "PUT" | "DELETE",
  data?: unknown,
  baseUrl?: string,
  outputFormat: "json" | "raw" = "json"
): string {
  const url = baseUrl || window.location.origin;
  const encodedPath = encodeURIComponent(path);
  const apiUrl = `${url}/api/application-services/${serviceId}/kv/${encodedPath}`;
  
  const parts: string[] = ["curl"];
  
  // Add method
  if (method !== "GET") {
    parts.push(`-X ${method}`);
  }
  
  // Add headers
  parts.push(`-H "Content-Type: application/json"`);
  parts.push(`-H "Authorization: Bearer <token>"`);
  
  // Add data for PUT
  if (method === "PUT" && data) {
    const dataStr = JSON.stringify(data, null, 2);
    // Escape quotes and newlines for curl
    const escapedData = dataStr.replace(/"/g, '\\"').replace(/\n/g, "\\n");
    parts.push(`-d "${escapedData}"`);
  }
  
  // Add output format
  if (outputFormat === "raw") {
    parts.push(`"${apiUrl}?raw=true"`);
  } else {
    parts.push(`"${apiUrl}"`);
  }
  
  return parts.join(" \\\n  ");
}

/**
 * Copy to clipboard with error handling
 * @param text - Text to copy
 * @returns Promise that resolves when copy is complete
 */
export async function copyToClipboard(text: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text);
  } catch (err) {
    // Fallback for older browsers
    const textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed";
    textArea.style.opacity = "0";
    document.body.appendChild(textArea);
    textArea.select();
    try {
      document.execCommand("copy");
    } catch (fallbackErr) {
      throw new Error("Failed to copy to clipboard");
    }
    document.body.removeChild(textArea);
  }
}

