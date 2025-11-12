import { useState, useEffect, useCallback } from "react";

export interface ConfigSearchEntry {
  application: string;
  profile: string;
  label?: string;
  timestamp: string;
}

const STORAGE_KEY = "config_search_history";
const MAX_ENTRIES = 20;

export function useConfigSearchHistory() {
  const [history, setHistory] = useState<ConfigSearchEntry[]>([]);

  // Load history from localStorage on mount
  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as ConfigSearchEntry[];
        setHistory(parsed);
      }
    } catch (error) {
      console.warn("Failed to load config search history:", error);
    }
  }, []);

  // Save history to localStorage
  const saveHistory = useCallback((entries: ConfigSearchEntry[]) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
      setHistory(entries);
    } catch (error) {
      console.warn("Failed to save config search history:", error);
    }
  }, []);

  // Add a new search entry
  const addSearch = useCallback(
    (application: string, profile: string, label?: string) => {
      const newEntry: ConfigSearchEntry = {
        application,
        profile,
        label,
        timestamp: new Date().toISOString(),
      };

      setHistory((prev) => {
        // Remove duplicates (same application, profile, label)
        const filtered = prev.filter(
          (entry) =>
            !(
              entry.application === application &&
              entry.profile === profile &&
              entry.label === label
            )
        );

        // Add new entry at the beginning
        const updated = [newEntry, ...filtered].slice(0, MAX_ENTRIES);
        saveHistory(updated);
        return updated;
      });
    },
    [saveHistory]
  );

  // Remove a specific entry
  const removeEntry = useCallback(
    (index: number) => {
      setHistory((prev) => {
        const updated = prev.filter((_, i) => i !== index);
        saveHistory(updated);
        return updated;
      });
    },
    [saveHistory]
  );

  // Clear all history
  const clearHistory = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEY);
      setHistory([]);
    } catch (error) {
      console.warn("Failed to clear config search history:", error);
    }
  }, []);

  // Get popular applications (most searched)
  const popularApplications = useCallback(() => {
    const appCounts = new Map<string, number>();
    history.forEach((entry) => {
      const count = appCounts.get(entry.application) || 0;
      appCounts.set(entry.application, count + 1);
    });

    return Array.from(appCounts.entries())
      .map(([application, count]) => ({ application, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10);
  }, [history]);

  return {
    history,
    addSearch,
    removeEntry,
    clearHistory,
    popularApplications,
  };
}

