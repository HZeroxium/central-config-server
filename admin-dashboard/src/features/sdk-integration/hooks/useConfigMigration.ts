/**
 * React hooks for configuration migration API integration.
 * Uses generated API hooks from @lib/api/generated/config-migration for type safety.
 */

import {
  useConvertIniToYaml as useConvertIniToYamlGenerated,
  useConvertPropertiesToYaml as useConvertPropertiesToYamlGenerated,
  useGeneratePropertiesClass as useGeneratePropertiesClassGenerated,
  useGenerateSdkConfig as useGenerateSdkConfigGenerated,
  useAnalyzeConfig as useAnalyzeConfigGenerated,
  useListTemplates as useListTemplatesGenerated,
} from '@lib/api/generated/config-migration/config-migration';
import { useMutation } from '@tanstack/react-query';
import { customInstance } from '@lib/api/mutator';
import { handleApiError } from '@lib/api/errorHandler';
import { logApiError } from '../utils/errorLogger';
import type {
  ConfigPropertiesGenerationResponse,
  SdkConfigGenerationResponse,
  ConfigAnalysisResponse,
  TemplateInfo,
} from '../types';
import type { IniToYamlRequest } from '@lib/api/models/iniToYamlRequest';
import type { PropertiesToYamlRequest } from '@lib/api/models/propertiesToYamlRequest';
import type { GeneratePropertiesRequest } from '@lib/api/models/generatePropertiesRequest';
import type { GenerateSdkConfigRequest } from '@lib/api/models/generateSdkConfigRequest';
import type { AnalyzeConfigRequest } from '@lib/api/models/analyzeConfigRequest';
import type { TemplateDetailResponse } from '@lib/api/models/templateDetailResponse';

/**
 * Hook for converting INI to YAML.
 */
export function useConvertIniToYaml() {
  const mutation = useConvertIniToYamlGenerated({
    mutation: {
      onError: (error) => {
        logApiError(error, '/api/migration/ini-to-yaml', 2);
        handleApiError(error, { silent: false });
      },
    },
  });
  
  return {
    ...mutation,
    mutateAsync: async (iniContent: string) => {
      try {
        const request: IniToYamlRequest = { iniContent };
        return await mutation.mutateAsync({ data: request });
      } catch (error) {
        logApiError(error, '/api/migration/ini-to-yaml', 2);
        throw error;
      }
    },
  };
}

/**
 * Hook for converting Properties to YAML.
 */
export function useConvertPropertiesToYaml() {
  const mutation = useConvertPropertiesToYamlGenerated({
    mutation: {
      onError: (error) => {
        logApiError(error, '/api/migration/properties-to-yaml', 2);
        handleApiError(error, { silent: false });
      },
    },
  });
  
  return {
    ...mutation,
    mutateAsync: async (propertiesContent: string) => {
      try {
        const request: PropertiesToYamlRequest = { propertiesContent };
        return await mutation.mutateAsync({ data: request });
      } catch (error) {
        logApiError(error, '/api/migration/properties-to-yaml', 2);
        throw error;
      }
    },
  };
}

/**
 * Hook for generating @ConfigurationProperties classes.
 */
export function useGeneratePropertiesClass() {
  const mutation = useGeneratePropertiesClassGenerated({
    mutation: {
      onError: (error) => {
        logApiError(error, '/api/migration/generate-properties-class', 3);
        handleApiError(error, { silent: false });
      },
    },
  });
  
  return {
    ...mutation,
    mutateAsync: async ({
      yamlContent,
      packageName,
    }: {
      yamlContent: string;
      packageName?: string;
    }): Promise<ConfigPropertiesGenerationResponse> => {
      try {
        const request: GeneratePropertiesRequest = {
          yamlContent,
          packageName,
        };
        return await mutation.mutateAsync({ data: request });
      } catch (error) {
        logApiError(error, '/api/migration/generate-properties-class', 3);
        throw error;
      }
    },
  };
}

/**
 * Hook for generating SDK configuration.
 */
export function useGenerateSdkConfig() {
  const mutation = useGenerateSdkConfigGenerated({
    mutation: {
      onError: (error) => {
        logApiError(error, '/api/migration/generate-sdk-config', 4);
        handleApiError(error, { silent: false });
      },
    },
  });
  
  return {
    ...mutation,
    mutateAsync: async ({
      applicationYml,
      serviceName,
    }: {
      applicationYml: string;
      serviceName?: string;
    }): Promise<SdkConfigGenerationResponse> => {
      try {
        const request: GenerateSdkConfigRequest = {
          applicationYml,
          serviceName,
        };
        return await mutation.mutateAsync({ data: request });
      } catch (error) {
        logApiError(error, '/api/migration/generate-sdk-config', 4);
        throw error;
      }
    },
  };
}

/**
 * Hook for analyzing configuration.
 */
export function useAnalyzeConfig() {
  const mutation = useAnalyzeConfigGenerated({
    mutation: {
      onError: (error) => {
        logApiError(error, '/api/migration/analyze-config', 2);
        handleApiError(error, { silent: true }); // Silent for auto-analysis
      },
    },
  });
  
  return {
    ...mutation,
    mutate: (variables: { configContent: string; format?: 'yaml' | 'properties' | 'ini' }) => {
      const request: AnalyzeConfigRequest = {
        configContent: variables.configContent,
        format: variables.format,
      };
      mutation.mutate({ data: request });
    },
    mutateAsync: async ({
      configContent,
      format,
    }: {
      configContent: string;
      format?: 'yaml' | 'properties' | 'ini';
    }): Promise<ConfigAnalysisResponse> => {
      try {
        const request: AnalyzeConfigRequest = {
          configContent,
          format,
        };
        return await mutation.mutateAsync({ data: request });
      } catch (error) {
        logApiError(error, '/api/migration/analyze-config', 2);
        throw error;
      }
    },
  };
}

/**
 * Hook for listing templates (query version for auto-fetching).
 */
export function useListTemplatesQuery() {
  const query = useListTemplatesGenerated();
  
  return {
    ...query,
    data: query.data?.templates || [],
  };
}

/**
 * Hook for listing templates (mutation version - wraps query for backward compatibility).
 */
export function useListTemplates() {
  const query = useListTemplatesGenerated();
  
  return {
    isPending: query.isPending,
    error: query.error,
    data: query.data?.templates || [],
    mutateAsync: async (): Promise<TemplateInfo[]> => {
      // This is a query, not a mutation, so we just return the data
      if (query.data?.templates) {
        return query.data.templates;
      }
      // If no data, wait for query to complete
      await query.refetch();
      return query.data?.templates || [];
    },
  };
}

/**
 * Hook for getting template details.
 * Uses custom mutation since the generated hook is a query but we need mutation-style API.
 */
export function useGetTemplate() {
  return useMutation({
    mutationFn: async (templateId: string): Promise<TemplateInfo> => {
      try {
        const response = await customInstance<TemplateDetailResponse>({
          url: `/api/migration/templates/${templateId}`,
          method: 'GET',
        });
        if (!response.template) {
          throw new Error('Template not found');
        }
        return response.template;
      } catch (error) {
        logApiError(error, `/api/migration/templates/${templateId}`, 0);
        handleApiError(error, { silent: false });
        throw error;
      }
    },
  });
}

