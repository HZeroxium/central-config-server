// src/lib/api/mutator.ts
import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios';
import { keycloak } from '@lib/keycloak/keycloakConfig';

const axiosInstance = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
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
  (error) => Promise.reject(error)
);

// Response interceptor với auto-unwrapping
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    // Auto-unwrap ApiResponse wrapper
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      return {
        ...response,
        data: response.data.data, // Unwrap the data
        meta: {
          status: response.data.status,
          message: response.data.message,
          timestamp: response.data.timestamp,
          traceId: response.data.traceId,
        }
      };
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      keycloak?.login();
    }
    return Promise.reject(error);
  }
);

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  return axiosInstance({
    ...config,
    ...options,
    headers: {
      ...config.headers,
      ...options?.headers,
    },
  }).then((response) => response.data);
};