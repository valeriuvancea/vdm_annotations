package org.vdm.overture;

import java.util.*;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.TypeName;

import org.overture.interpreter.values.*;

public class VDMTypesHelper {
    public static final List<Class<?>> supportedJavaClasses = new ArrayList<Class<?>>() {
        {
            add(byte.class);
            add(short.class);
            add(int.class);
            add(long.class);
            add(float.class);
            add(double.class);
            add(char.class);
            add(boolean.class);
            add(Byte.class);
            add(Short.class);
            add(Integer.class);
            add(Long.class);
            add(Float.class);
            add(Double.class);
            add(Character.class);
            add(Boolean.class);
            add(CharSequence.class);
            add(String.class);
        }
    };

    public static final List<Class<?>> supportedVDMClasses = new ArrayList<Class<?>>() {
        {
            add(BooleanValue.class);
            add(CharacterValue.class);
            add(IntegerValue.class);
            add(MapValue.class);
            add(NaturalValue.class);
            add(QuoteValue.class);
            add(RationalValue.class);
            add(RealValue.class);
            add(SeqValue.class);
            add(SetValue.class);
        }
    };

    // For the aspect
    public static <T> String getVDMStringFromJavaValue(T value) throws Exception {
        Class<?> valuesClass = value.getClass();
        Exception unsupportedTypeException = new Exception("Unsupported provided type: " + value.getClass().toString());

        if (supportedJavaClasses.contains(valuesClass)) {
            return getVDMStringFromValue(value);
        } else if (valuesClass.isArray() && supportedJavaClasses.contains(valuesClass.getComponentType())) {
            return getVDMStringFromArrayValue(value);
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            boolean wasFirstEntryVerified = false;
            String result = "{";
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!wasFirstEntryVerified) {
                    result += ", ";
                } else {
                    if (!supportedJavaClasses.contains(entry.getKey().getClass())
                            || !supportedJavaClasses.contains(entry.getValue().getClass())) {
                        throw unsupportedTypeException;
                    }
                    wasFirstEntryVerified = true;
                }
                result += getVDMStringFromValue(entry.getKey()) + " |-> " + getVDMStringFromValue(entry.getValue());
            }
            result += "}";
            return result;
        } else if (value instanceof Collection) {
            Set<?> collection = (Set<?>) value;
            boolean wasFirstEntryVerified = false;
            String result = value instanceof Set ? "{" : "[";
            Iterator<?> iterator = collection.iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();
                if (!wasFirstEntryVerified) {
                    result += ", ";
                } else {
                    if (!supportedJavaClasses.contains(item.getClass())) {
                        throw unsupportedTypeException;
                    }
                    wasFirstEntryVerified = true;
                }
                result += getVDMStringFromValue(iterator);
            }
            result += value instanceof Set ? "}" : "]";
            return result;
        } else {
            throw unsupportedTypeException;
        }
    }

    private static <T> String getVDMStringFromValue(T value) {
        if (value.getClass() == char.class || value.getClass() == Character.class) {
            return "'" + value.toString() + "'";
        } else if (value.getClass() == String.class || value.getClass() == CharSequence.class) {
            return "\"" + value.toString() + "\"";
        } else {
            return value.toString();
        }
    }

    private static <T> String getVDMStringFromArrayValue(T value) {
        if (value.getClass() == char[].class || value.getClass() == Character[].class) {
            StringBuilder builder = new StringBuilder();
            for (Object character : (Object[]) value) {
                builder.append(character);
            }
            return "\"" + builder.toString() + "\"";
        } else {
            return Arrays.toString((Object[]) value);
        }
    }

    // For the return value
    public static <ProvidedType> Value getVDMValueFromJavaValue(ProvidedType value) {
        return null;
    }

    // For the java code generation method arguments
    public static TypeName getVDMTypeNameFromJavaType(TypeName type) throws Exception {
        List<TypeName> integerTypes = new ArrayList<TypeName>() {
            {
                add(TypeName.get(byte.class));
                add(TypeName.get(short.class));
                add(TypeName.get(int.class));
                add(TypeName.get(long.class));
                add(TypeName.get(Byte.class));
                add(TypeName.get(Short.class));
                add(TypeName.get(Integer.class));
                add(TypeName.get(Long.class));
            }
        };

        List<TypeName> realTypes = new ArrayList<TypeName>() {
            {
                add(TypeName.get(float.class));
                add(TypeName.get(double.class));
                add(TypeName.get(Float.class));
                add(TypeName.get(Double.class));
            }
        };

        List<TypeName> charTypes = new ArrayList<TypeName>() {
            {
                add(TypeName.get(char.class));
                add(TypeName.get(Character.class));
            }
        };

        List<TypeName> booleanTypes = new ArrayList<TypeName>() {
            {
                add(TypeName.get(boolean.class));
                add(TypeName.get(Boolean.class));
            }
        };

        List<TypeName> sequenceTypes = new ArrayList<TypeName>() {
            {
                add(TypeName.get(CharSequence.class));
                add(TypeName.get(String.class));
            }
        };

        Exception unsupportedTypeException = new Exception("Unsupported provided type: " + type.toString());

        Class<?> classToReturn = null;
        if (integerTypes.contains(type)) {
            classToReturn = IntegerValue.class;
        } else if (realTypes.contains(type)) {
            classToReturn = RealValue.class;
        } else if (charTypes.contains(type)) {
            classToReturn = CharacterValue.class;
        } else if (booleanTypes.contains(type)) {
            classToReturn = BooleanValue.class;
        } else if (sequenceTypes.contains(type) || type instanceof ArrayTypeName) {
            classToReturn = SeqValue.class;
        } else {
            Class<?> providedClassType = Class.forName(type.toString().substring(0, type.toString().indexOf("<")));
            if (Set.class.isAssignableFrom(providedClassType)) {
                classToReturn = SetValue.class;
            } else if (Collection.class.isAssignableFrom(providedClassType)) {
                classToReturn = SeqValue.class;
            } else if (Map.class.isAssignableFrom(providedClassType)) {
                classToReturn = MapValue.class;
            } else {
                throw unsupportedTypeException;
            }
        }

        return TypeName.get(classToReturn);

    }

    // For the java code generation super class method arguments
    public static <ProvidedType extends Value, ReturnType> ReturnType getJavaValueFromVDMValue(ProvidedType value,
            String className) throws Exception {
        return (ReturnType) null;
    }
}
