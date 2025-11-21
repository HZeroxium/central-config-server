package com.example.control.infrastructure.configmigration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating Jakarta Validation annotations
 * based on field type and value.
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class ValidationAnnotationGenerator {

    /**
     * Generates validation annotations for a field.
     *
     * @param fieldName  the field name
     * @param fieldType  the Java type
     * @param nullable   whether the field is nullable
     * @param value      the default value (for min/max inference)
     * @return list of validation annotation strings
     */
    public List<String> generateAnnotations(String fieldName, String fieldType, boolean nullable, Object value) {
        List<String> annotations = new ArrayList<>();

        // Nullability annotations
        if (!nullable) {
            if (fieldType.equals("String")) {
                annotations.add("@NotBlank");
            } else {
                annotations.add("@NotNull");
            }
        }

        // Type-specific validations
        if (fieldType.equals("Integer") || fieldType.equals("Long")) {
            if (value instanceof Number) {
                Number num = (Number) value;
                if (num.intValue() >= 0) {
                    annotations.add("@Min(0)");
                } else {
                    annotations.add("@Min(0) // TODO: Adjust minimum value");
                }
            } else {
                annotations.add("@Min(0) // TODO: Set appropriate minimum");
            }
        }

        if (fieldType.equals("Double") || fieldType.equals("Float")) {
            if (value instanceof Number) {
                Number num = (Number) value;
                if (num.doubleValue() >= 0.0) {
                    annotations.add("@DecimalMin(value = \"0.0\", inclusive = true)");
                }
            }
        }

        // String-specific validations
        if (fieldType.equals("String") && value instanceof String) {
            String str = (String) value;
            if (str.length() > 0 && str.length() < 50) {
                // Could add @Size, but usually not needed for config properties
            }
        }

        // Boolean fields don't need validation annotations typically

        return annotations;
    }

    /**
     * Generates import statements for validation annotations.
     *
     * @param annotations the annotations used
     * @return list of import statements
     */
    public List<String> generateImports(List<String> annotations) {
        List<String> imports = new ArrayList<>();

        if (annotations.stream().anyMatch(a -> a.contains("@NotBlank"))) {
            imports.add("import jakarta.validation.constraints.NotBlank;");
        }
        if (annotations.stream().anyMatch(a -> a.contains("@NotNull"))) {
            imports.add("import jakarta.validation.constraints.NotNull;");
        }
        if (annotations.stream().anyMatch(a -> a.contains("@Min"))) {
            imports.add("import jakarta.validation.constraints.Min;");
        }
        if (annotations.stream().anyMatch(a -> a.contains("@DecimalMin"))) {
            imports.add("import jakarta.validation.constraints.DecimalMin;");
        }
        if (annotations.stream().anyMatch(a -> a.contains("@Size"))) {
            imports.add("import jakarta.validation.constraints.Size;");
        }
        if (annotations.stream().anyMatch(a -> a.contains("@Valid"))) {
            imports.add("import jakarta.validation.Valid;");
        }

        return imports;
    }
}

