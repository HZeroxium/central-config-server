// src/lib/api/mutator.ts
import axios, { type AxiosRequestConfig, type AxiosResponse } from "axios";
import { keycloak } from "@lib/keycloak/keycloakConfig";

// Use environment variable for API base URL
const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";

console.log("API Base URL:", API_BASE_URL);

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = keycloak?.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) =>
    Promise.reject(error instanceof Error ? error : new Error(String(error)))
);

// Response interceptor with auto-unwrapping
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    // Auto-unwrap ApiResponse wrapper
    if (
      response.data &&
      typeof response.data === "object" &&
      "data" in response.data
    ) {
      return {
        ...response,
        data: response.data.data, // Unwrap the data
        meta: {
          status: response.data.status,
          message: response.data.message,
          timestamp: response.data.timestamp,
          traceId: response.data.traceId,
        },
      };
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      keycloak?.login();
    }
    return Promise.reject(
      error instanceof Error ? error : new Error(String(error))
    );
  }
);

/**
 * Wrapper for customInstance with request deduplication
 * TanStack Query already handles request deduplication at the query level,
 * so this is mainly for manual axios calls outside of React Query
 */
const requestCache = new Map<string, Promise<any>>();

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig
): Promise<T> => {
  // Generate cache key for deduplication
  const cacheKey = `${config.method || "GET"}:${config.url}:${JSON.stringify(
    options?.params || config.params
  )}:${JSON.stringify(options?.data || config.data)}`;

  // Check if an identical request is already in progress
  const cachedRequest = requestCache.get(cacheKey);
  if (cachedRequest) {
    return cachedRequest;
  }

  // Create new request
  const requestPromise = axiosInstance({
    ...config,
    ...options,
    headers: {
      ...config.headers,
      ...options?.headers,
    },
  })
    .then((response) => response.data)
    .finally(() => {
      // Clean up cache after request completes
      requestCache.delete(cacheKey);
    });

  // Cache the request promise
  requestCache.set(cacheKey, requestPromise);

  return requestPromise;
};
