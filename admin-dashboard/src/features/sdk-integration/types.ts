/**
 * TypeScript types for SDK integration wizard state and API responses.
 * Uses generated API types from @lib/api/models for type safety.
 */

import type { GeneratedClass } from '@lib/api/models/generatedClass';
import type { ConfigPropertiesGenerationResponse } from '@lib/api/models/configPropertiesGenerationResponse';
import type { SdkConfigGenerationResponse } from '@lib/api/models/sdkConfigGenerationResponse';
import type { ConfigAnalysisResponse } from '@lib/api/models/configAnalysisResponse';
import type { ValidationResult } from '@lib/api/models/validationResult';
import type { LintingIssue } from '@lib/api/models/lintingIssue';
import type { PropertyGroupSuggestion } from '@lib/api/models/propertyGroupSuggestion';
import type { TemplateInfo } from '@lib/api/models/templateInfo';
import type { PropertiesToYamlResponse } from '@lib/api/models/propertiesToYamlResponse';
import type { IniToYamlResponse } from '@lib/api/models/iniToYamlResponse';

export type ConfigFormat = 'yaml' | 'properties' | 'ini';

export interface WizardState {
  step: number;
  configFile: File | null;
  configContent: string;
  configFormat: ConfigFormat;
  convertedYaml: string | null;
  generatedPropertiesClasses: GeneratedClass[] | null;
  generatedSdkConfig: SdkConfigGenerationResponse | null;
  analysisResult: ConfigAnalysisResponse | null;
}

// Re-export generated types for convenience
export type { GeneratedClass };
export type { ConfigPropertiesGenerationResponse };
export type { SdkConfigGenerationResponse };
export type { ConfigAnalysisResponse };
export type { ValidationResult };
export type { LintingIssue };
export type { PropertyGroupSuggestion };
export type { TemplateInfo };

// Type aliases for conversion results (can be either PropertiesToYamlResponse or IniToYamlResponse)
export type ConversionResult = PropertiesToYamlResponse | IniToYamlResponse;

// Type guard to check if result is from Properties conversion
export function isPropertiesConversionResult(
  result: ConversionResult
): result is PropertiesToYamlResponse {
  return 'yamlContent' in result;
}

// Type guard to check if result is from INI conversion
export function isIniConversionResult(
  result: ConversionResult
): result is IniToYamlResponse {
  return 'yamlContent' in result;
}

