// Type helpers for form error handling
import type { FieldErrors, FieldValues, Path } from "react-hook-form";

export function getFieldError<T extends FieldValues>(
  errors: FieldErrors<T>,
  field: Path<T>
): string | undefined {
  const error = errors[field];
  return error?.message as string | undefined;
}

export function hasFieldError<T extends FieldValues>(
  errors: FieldErrors<T>,
  field: Path<T>
): boolean {
  return !!errors[field];
}
