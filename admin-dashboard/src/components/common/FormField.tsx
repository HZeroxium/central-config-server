import { Controller, type Control, type FieldPath, type FieldValues } from 'react-hook-form';
import { TextField, type TextFieldProps } from '@mui/material';

interface FormFieldProps<T extends FieldValues> extends Omit<TextFieldProps, 'name' | 'control'> {
  name: FieldPath<T>;
  control: Control<T>;
  label: string;
  required?: boolean;
}

export const FormField = <T extends FieldValues>({
  name,
  control,
  label,
  required = false,
  ...textFieldProps
}: FormFieldProps<T>) => {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <TextField
          {...field}
          {...textFieldProps}
          label={label}
          required={required}
          error={!!error}
          helperText={error?.message}
          fullWidth
          variant="outlined"
        />
      )}
    />
  );
};

export default FormField;
