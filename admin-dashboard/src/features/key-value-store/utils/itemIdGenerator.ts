/**
 * Utility functions for generating unique itemIds for KV LIST items.
 * 
 * Format: `item-${timestamp}-${random}`
 * Example: `item-1704067200000-a1b2c3`
 * 
 * Rationale:
 * - Unique (timestamp + random)
 * - Sortable (timestamp first)
 * - Human-readable prefix
 * - Short enough for UI
 */

/**
 * Generates a random hexadecimal string of specified length.
 * 
 * @param length Length of the random string (default: 6)
 * @returns Random hexadecimal string
 */
function generateRandomHex(length: number = 6): string {
  const chars = "0123456789abcdef";
  let result = "";
  for (let i = 0; i < length; i++) {
    result += chars[Math.floor(Math.random() * chars.length)];
  }
  return result;
}

/**
 * Generates a unique itemId.
 * 
 * Format: `item-${timestamp}-${random}`
 * 
 * @returns Unique itemId string
 */
export function generateItemId(): string {
  const timestamp = Date.now();
  const random = generateRandomHex(6);
  return `item-${timestamp}-${random}`;
}

/**
 * Validates if a string matches the itemId format.
 * 
 * @param id The string to validate
 * @returns True if valid, false otherwise
 */
export function isValidItemId(id: string): boolean {
  if (!id || typeof id !== "string") {
    return false;
  }
  // Format: item-{timestamp}-{random}
  const pattern = /^item-\d+-[0-9a-f]{6}$/;
  return pattern.test(id);
}

/**
 * Checks if an itemId already exists in a list of items.
 * 
 * @param id The itemId to check
 * @param existingItems Array of existing items with id property
 * @returns True if duplicate, false otherwise
 */
export function isDuplicateItemId(
  id: string,
  existingItems: Array<{ id: string }>
): boolean {
  return existingItems.some((item) => item.id === id);
}

/**
 * Generates a unique itemId that doesn't exist in the given list.
 * 
 * @param existingItems Array of existing items with id property
 * @param maxAttempts Maximum number of attempts to generate unique ID (default: 10)
 * @returns Unique itemId string
 */
export function generateUniqueItemId(
  existingItems: Array<{ id: string }> = [],
  maxAttempts: number = 10
): string {
  let attempts = 0;
  let itemId: string;
  
  do {
    itemId = generateItemId();
    attempts++;
    
    if (attempts > maxAttempts) {
      // Fallback: add counter to ensure uniqueness
      const timestamp = Date.now();
      const random = generateRandomHex(6);
      const counter = attempts;
      itemId = `item-${timestamp}-${random}-${counter}`;
      break;
    }
  } while (isDuplicateItemId(itemId, existingItems));
  
  return itemId;
}

