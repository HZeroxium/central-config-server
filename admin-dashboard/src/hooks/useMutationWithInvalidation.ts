import { useQueryClient, type UseMutationOptions } from "@tanstack/react-query";
import type { QueryKey } from "@tanstack/react-query";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";

export interface MutationWithInvalidationOptions<
  TData,
  TError,
  TVariables,
  TContext
> {
  /**
   * Query keys or query key factories to invalidate on success.
   * Can be a single key, array of keys, or a function that returns keys based on variables/context.
   */
  invalidateQueries?:
    | QueryKey[]
    | ((
        data: TData,
        variables: TVariables,
        context: TContext | undefined
      ) => QueryKey[]);

  /**
   * Whether to invalidate queries before the mutation succeeds (optimistic updates).
   */
  invalidateOnMutate?: boolean;

  /**
   * Custom success message. If not provided, uses default success toast.
   */
  successMessage?: string | ((data: TData, variables: TVariables) => string);

  /**
   * Whether to show success toast. Defaults to true.
   */
  showSuccessToast?: boolean;

  /**
   * Whether to handle errors automatically. Defaults to true.
   */
  handleError?: boolean;

  /**
   * Custom onSuccess callback. Called after invalidation and success toast.
   */
  onSuccess?: (
    data: TData,
    variables: TVariables,
    context: TContext | undefined
  ) => void | Promise<void>;

  /**
   * Custom onError callback. Called after error handling.
   */
  onError?: (
    error: TError,
    variables: TVariables,
    context: TContext | undefined
  ) => void | Promise<void>;
}

/**
 * Hook that creates enhanced mutation options with automatic query invalidation and consistent error handling.
 *
 * Use this helper to wrap mutation options before passing to useMutation hooks.
 *
 * @example
 * ```tsx
 * const mutationOptions = useMutationWithInvalidation({
 *   invalidateQueries: (data, variables) => [
 *     getFindApprovalRequestByIdQueryKey(variables.id),
 *     getFindApprovalDecisionsByRequestIdQueryKey(variables.id),
 *     getFindAllApprovalRequestsQueryKey(),
 *   ],
 *   successMessage: 'Decision submitted successfully',
 * });
 *
 * const mutation = useSubmitApprovalDecision(mutationOptions);
 * ```
 */
export function useMutationWithInvalidation<
  TData,
  TError,
  TVariables,
  TContext
>(
  options: MutationWithInvalidationOptions<
    TData,
    TError,
    TVariables,
    TContext
  > = {}
): Partial<UseMutationOptions<TData, TError, TVariables, TContext>> {
  const queryClient = useQueryClient();

  const {
    invalidateQueries,
    invalidateOnMutate = false,
    successMessage,
    showSuccessToast = true,
    handleError = true,
    onSuccess,
    onError,
  } = options;

  const mutationOptions: Partial<
    UseMutationOptions<TData, TError, TVariables, TContext>
  > = {
    onSuccess: async (data, variables, context) => {
      // Invalidate queries after successful mutation
      if (invalidateQueries) {
        const keys =
          typeof invalidateQueries === "function"
            ? invalidateQueries(data, variables, context)
            : invalidateQueries;

        keys.forEach((key) => {
          queryClient.invalidateQueries({ queryKey: key });
        });
      }

      // Show success toast
      if (showSuccessToast) {
        const message =
          typeof successMessage === "function"
            ? successMessage(data, variables)
            : successMessage || "Operation completed successfully";

        toast.success(message);
      }

      // Call custom onSuccess callback
      await onSuccess?.(data, variables, context);
    },
    onError: async (error, variables, context) => {
      // Handle error automatically if enabled
      if (handleError) {
        handleApiError(error);
      }

      // Call custom onError callback
      await onError?.(error, variables, context);
    },
  };

  // Only add onMutate if optimistic updates are enabled
  if (invalidateOnMutate && invalidateQueries) {
    mutationOptions.onMutate = async (variables) => {
      const keys =
        typeof invalidateQueries === "function"
          ? invalidateQueries(
              undefined as unknown as TData,
              variables,
              undefined
            )
          : invalidateQueries;

      keys.forEach((key) => {
        queryClient.invalidateQueries({ queryKey: key });
      });
      return undefined as unknown as TContext;
    };
  }

  return mutationOptions;
}
