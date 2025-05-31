package com.zomdroid.agent.glsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GLSLType {
    abstract String getName();

    static class PrimitiveType extends GLSLType {
        private final String name;

        public PrimitiveType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static class StructType extends GLSLType {
        private final String name;  // e.g., "Material", "Light"
        private final Map<String, GLSLType> fields = new HashMap<>();

        public StructType(String name, String[] fieldNames, GLSLType[] fieldTypes) {
            this.name = name;
            if (fieldNames.length != fieldTypes.length)
                throw new RuntimeException("Different number of fields and types");
            for (int i = 0; i < fieldNames.length; i++) {
                this.fields.put(fieldNames[i], fieldTypes[i]);
            }
        }

        public StructType(String name) {
            this.name = name;
        }

        public void addField(String fieldName, GLSLType fieldType) {
            fields.put(fieldName, fieldType);
        }

        public GLSLType getFieldType(String fieldName) {
            return fields.get(fieldName);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static class VectorType extends StructType {
        public static final ArrayList<Character> VECTOR_FIELDS = new ArrayList<>(List.of('x', 'y', 'z', 'w', 'r', 'g', 'b', 'a', 's', 't', 'p', 'q'));
        private final int numFields;
        private final GLSLType baseType;
        VectorType(GLSLType baseType, int numFields, String prefix) {
            super(prefix + numFields);
            this.baseType = baseType;
            this.numFields = numFields;
            if (numFields < 2 || numFields > 4)
                throw new RuntimeException("Number of fields in vector type must be between 2 and 4");
        }

        public static VectorType from(GLSLType baseType, int numFields) {
            String prefix = null;
            if (baseType.getName().equals("float")) {
                prefix = "vec";
            } else if (baseType.getName().equals("bool")) {
                prefix = "bvec";
            } else if (baseType.getName().equals("int")) {
                prefix = "ivec";
            } else {
                throw new RuntimeException("Unknown base type: " + baseType.getName());
            }
            return new VectorType(baseType, numFields, prefix);
        }

        @Override
        public void addField(String fieldName, GLSLType fieldType) {
            throw new UnsupportedOperationException("Adding fields to vector type is not allowed");
        }

        public GLSLType getFieldType(String swizzle) {
            if (swizzle.isEmpty())
                throw new IllegalArgumentException("Swizzle cannot be empty");

            for (char c : swizzle.toCharArray()) {
                int i = VECTOR_FIELDS.indexOf(c);
                if (i == -1)
                    throw new IllegalArgumentException("Invalid swizzle character '" + c + "'");

                if (i % 4 >= this.numFields)
                    throw new IllegalArgumentException("Swizzle component " + c + " out of bounds for vector of size " + this.numFields);
            }

            return swizzle.length() == 1
                    ? baseType
                    : VectorType.from(baseType, swizzle.length());
        }
    }
}

