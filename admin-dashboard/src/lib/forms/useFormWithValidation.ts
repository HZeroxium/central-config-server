import { useForm, type UseFormProps, type UseFormReturn, type FieldValues, type Path } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { AxiosError } from 'axios';
import type { ErrorResponse } from '@lib/api/models';

 
interface UseFormWithValidationProps<TFormValues extends FieldValues>
  extends Omit<UseFormProps<TFormValues>, 'resolver'> {
  // Using any here to avoid zodResolver type complexity
  schema: any;
}

interface UseFormWithValidationReturn<TFormValues extends FieldValues>
  extends UseFormReturn<TFormValues> {
  setServerErrors: (error: unknown) => void;
}

/**
 * Custom hook that wraps react-hook-form with Zod validation
 * and provides utilities for handling server-side validation errors
 */
export function useFormWithValidation<TFormValues extends FieldValues>(
  props: UseFormWithValidationProps<TFormValues>
): UseFormWithValidationReturn<TFormValues> {
  const { schema, ...formProps } = props;

  const form = useForm<TFormValues>({
    ...formProps,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(schema) as any,
  });

  /**
   * Set server-side validation errors on form fields
   */
  const setServerErrors = (error: unknown) => {
    if (error instanceof AxiosError && error.response?.status === 400) {
      const errorData = error.response.data as ErrorResponse;

      // Handle validation errors from backend
      if (errorData?.validationErrors) {
        errorData.validationErrors.forEach((validationError) => {
          if (validationError.field) {
            const fieldName = validationError.field as Path<TFormValues>;
            form.setError(fieldName, {
              type: 'server',
              message: validationError.message || 'Invalid value',
            });
          }
        });
      }

      // Handle generic error message
      if (errorData?.detail && !errorData?.validationErrors?.length) {
        form.setError('root', {
          type: 'server',
          message: errorData.detail,
        });
      }
    } else {
      // For non-validation errors, set a generic root error
      form.setError('root', {
        type: 'server',
        message: error instanceof Error ? error.message : 'An unexpected error occurred',
      });
    }
  };

  return {
    ...form,
    setServerErrors,
  };
}

/**
 * Helper to check if there are any server errors in the form
 */
export function hasServerErrors<TFormValues extends FieldValues>(
  form: UseFormReturn<TFormValues>
): boolean {
  const errors = form.formState.errors;
  return Object.values(errors).some((error) => error?.type === 'server');
}

/**
 * Helper to get all server error messages
 */
export function getServerErrorMessages<TFormValues extends FieldValues>(
  form: UseFormReturn<TFormValues>
): string[] {
  const errors = form.formState.errors;
  return Object.values(errors)
    .filter((error) => error && error.type === 'server')
    .map((error) => error?.message || 'Server error')
    .filter((msg): msg is string => Boolean(msg));
}
