/**
 * Hook for managing SDK integration wizard state.
 */

import { useState, useCallback, useEffect } from 'react';
import type {
  WizardState,
  ConfigFormat,
  GeneratedClass,
  SdkConfigGenerationResponse,
  ConfigAnalysisResponse,
} from '../types';

const STORAGE_KEY = 'sdk-integration-wizard-state';

/**
 * Hook for managing wizard state with localStorage persistence.
 */
export function useMigrationWizard() {
  const [state, setState] = useState<WizardState>(() => {
    // Try to restore from localStorage
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        // Don't restore file object (can't be serialized)
        return {
          ...parsed,
          configFile: null,
        };
      }
    } catch (e) {
      // Ignore parse errors
    }
    
    return {
      step: 1,
      configFile: null,
      configContent: '',
      configFormat: 'yaml',
      convertedYaml: null,
      generatedPropertiesClasses: null,
      generatedSdkConfig: null,
      analysisResult: null,
    };
  });

  // Persist state to localStorage
  useEffect(() => {
    try {
      const toSave = {
        ...state,
        configFile: null, // Don't save file object
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify(toSave));
    } catch (e) {
      // Ignore storage errors
    }
  }, [state]);

  const setStep = useCallback((step: number) => {
    setState(prev => ({ ...prev, step }));
  }, []);

  const setConfigFile = useCallback((file: File | null) => {
    setState(prev => ({ ...prev, configFile: file }));
  }, []);

  const setConfigContent = useCallback((content: string) => {
    setState(prev => ({ ...prev, configContent: content }));
  }, []);

  const setConfigFormat = useCallback((format: ConfigFormat) => {
    setState(prev => ({ ...prev, configFormat: format }));
  }, []);

  const setConvertedYaml = useCallback((yaml: string | null) => {
    setState(prev => ({ ...prev, convertedYaml: yaml }));
  }, []);

  const setGeneratedPropertiesClasses = useCallback((classes: GeneratedClass[] | null) => {
    setState(prev => ({ ...prev, generatedPropertiesClasses: classes }));
  }, []);

  const setGeneratedSdkConfig = useCallback((config: SdkConfigGenerationResponse | null) => {
    setState(prev => ({ ...prev, generatedSdkConfig: config }));
  }, []);

  const setAnalysisResult = useCallback((result: ConfigAnalysisResponse | null) => {
    setState(prev => ({ ...prev, analysisResult: result }));
  }, []);

  const reset = useCallback(() => {
    setState({
      step: 1,
      configFile: null,
      configContent: '',
      configFormat: 'yaml',
      convertedYaml: null,
      generatedPropertiesClasses: null,
      generatedSdkConfig: null,
      analysisResult: null,
    });
    localStorage.removeItem(STORAGE_KEY);
  }, []);

  const nextStep = useCallback(() => {
    setState(prev => ({ ...prev, step: Math.min(prev.step + 1, 5) }));
  }, []);

  const previousStep = useCallback(() => {
    setState(prev => ({ ...prev, step: Math.max(prev.step - 1, 1) }));
  }, []);

  return {
    state,
    setStep,
    setConfigFile,
    setConfigContent,
    setConfigFormat,
    setConvertedYaml,
    setGeneratedPropertiesClasses,
    setGeneratedSdkConfig,
    setAnalysisResult,
    reset,
    nextStep,
    previousStep,
  };
}

