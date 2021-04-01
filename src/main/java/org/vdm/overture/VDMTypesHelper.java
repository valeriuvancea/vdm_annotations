package org.vdm.overture;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.TypeName;

import org.overture.interpreter.values.*;

@SuppressWarnings({ "serial", "unchecked" })
public class VDMTypesHelper {
    public static final Set<Class<?>> supportedJavaClasses = new HashSet<Class<?>>() {
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
            add(String.class);
        }
    };

    public static final Set<Class<?>> supportedVDMClasses = new HashSet<Class<?>>() {
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

    public static final Set<TypeName> javaVDMEquivalentIntegerTypes = new HashSet<TypeName>() {
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

    public static final Set<TypeName> javaVDMEquivalentRealTypes = new HashSet<TypeName>() {
        {
            add(TypeName.get(float.class));
            add(TypeName.get(double.class));
            add(TypeName.get(Float.class));
            add(TypeName.get(Double.class));
        }
    };

    public static final Set<TypeName> javaVDMEquivalentCharTypes = new HashSet<TypeName>() {
        {
            add(TypeName.get(char.class));
            add(TypeName.get(Character.class));
        }
    };

    public static final Set<TypeName> javaVDMEquivalentBooleanTypes = new HashSet<TypeName>() {
        {
            add(TypeName.get(boolean.class));
            add(TypeName.get(Boolean.class));
        }
    };

    public static final Set<TypeName> javaVDMEquivalentCharSequenceTypes = new HashSet<TypeName>() {
        {
            add(TypeName.get(String.class));
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
            return getVDMStringFromJavaMapValue(value, unsupportedTypeException);
        } else if (value instanceof Collection) {
            return getVDMStringFromJavaCollectionValue(value, unsupportedTypeException);
        } else {
            throw unsupportedTypeException;
        }
    }

    // For the return value
    public static <ProvidedType> Value getVDMValueFromJavaValue(ProvidedType value) throws Exception {
        Class<?> providedType = value.getClass();
        TypeName providedTypeName = TypeName.get(providedType);
        String stringValue = value.toString();
        Exception unsupportedTypeException = new Exception("Unsupported provided type: " + providedTypeName.toString());

        if (javaVDMEquivalentIntegerTypes.contains(providedTypeName)) {
            return new IntegerValue(Long.parseLong(stringValue));
        } else if (javaVDMEquivalentRealTypes.contains(providedTypeName)) {
            return new RealValue(Double.parseDouble(stringValue));
        } else if (javaVDMEquivalentCharTypes.contains(providedTypeName)) {
            return new CharacterValue(stringValue.charAt(0));
        } else if (javaVDMEquivalentBooleanTypes.contains(providedTypeName)) {
            return new BooleanValue(Boolean.parseBoolean(stringValue));
        } else if (javaVDMEquivalentCharSequenceTypes.contains(providedTypeName)) {
            ValueList vdmStringValue = new ValueList();

            for (char character : stringValue.toCharArray()) {
                vdmStringValue.add(new CharacterValue(character));
            }

            return new SeqValue(vdmStringValue);
        } else if (value instanceof Set) {
            ValueSet vdmSetValues = new ValueSet();
            Iterator<?> iterator = ((Set<?>) value).iterator();

            while (iterator.hasNext()) {
                vdmSetValues.add(getVDMValueFromJavaValue(iterator.next()));
            }

            return new SetValue(vdmSetValues);
        } else if (value instanceof Collection) {
            ValueList vdmSeqValues = new ValueList();
            Iterator<?> iterator = ((Collection<?>) value).iterator();

            while (iterator.hasNext()) {
                vdmSeqValues.add(getVDMValueFromJavaValue(iterator.next()));
            }

            return new SeqValue(vdmSeqValues);
        } else if (value instanceof Map) {
            ValueMap vdmMapValues = new ValueMap();

            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                vdmMapValues.put(getVDMValueFromJavaValue(entry.getKey()), getVDMValueFromJavaValue(entry.getValue()));
            }

            return new MapValue(vdmMapValues);
        } else if (providedType.isArray()) {
            return getVDMValueFromJavaValue(Arrays.asList(value));
        } else {
            throw unsupportedTypeException;
        }
    }

    // For the java code generation method arguments
    public static TypeName getVDMTypeNameFromJavaType(TypeName type) throws Exception {
        Exception unsupportedTypeException = new Exception("Unsupported provided type: " + type.toString());

        Class<?> classToReturn = null;
        if (javaVDMEquivalentIntegerTypes.contains(type)) {
            classToReturn = IntegerValue.class;
        } else if (javaVDMEquivalentRealTypes.contains(type)) {
            classToReturn = RealValue.class;
        } else if (javaVDMEquivalentCharTypes.contains(type)) {
            classToReturn = CharacterValue.class;
        } else if (javaVDMEquivalentBooleanTypes.contains(type)) {
            classToReturn = BooleanValue.class;
        } else if (javaVDMEquivalentCharSequenceTypes.contains(type) || type instanceof ArrayTypeName) {
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

    // For vdm code generation
    public static String getVDMTypeAsStringFromJavaType(TypeName type) throws Exception {
        String typeString = type.toString();
        Exception unsupportedTypeException = new Exception("Unsupported provided type: " + typeString);

        String typeToReturn = null;
        if (type.toString().equals("void")) {
            return "";
        } else if (javaVDMEquivalentIntegerTypes.contains(type)) {
            typeToReturn = "int";
        } else if (javaVDMEquivalentRealTypes.contains(type)) {
            typeToReturn = "real";
        } else if (javaVDMEquivalentCharTypes.contains(type)) {
            typeToReturn = "char";
        } else if (javaVDMEquivalentBooleanTypes.contains(type)) {
            typeToReturn = "bool";
        } else if (javaVDMEquivalentCharSequenceTypes.contains(type)) {
            typeToReturn = "seq of char";
        } else if (type instanceof ArrayTypeName) {
            String arrayType = typeString.substring(0, typeString.indexOf("["));
            typeToReturn = "seq of " + getVDMTypeAsStringFromJavaType(TypeName.get(getClassFromString(arrayType)));
        } else {
            Class<?> providedClassType = Class.forName(typeString.substring(0, typeString.indexOf("<")));
            String genericTypeString = typeString.substring(typeString.indexOf("<") + 1, typeString.indexOf(">"));

            if (Set.class.isAssignableFrom(providedClassType)) {
                typeToReturn = "set of "
                        + getVDMTypeAsStringFromJavaType(TypeName.get(getClassFromString(genericTypeString)));
            } else if (Collection.class.isAssignableFrom(providedClassType)) {
                typeToReturn = "seq of "
                        + getVDMTypeAsStringFromJavaType(TypeName.get(getClassFromString(genericTypeString)));
            } else if (Map.class.isAssignableFrom(providedClassType)) {
                String genericKeyType = genericTypeString.substring(0, genericTypeString.indexOf(","));
                String genericValueType = genericTypeString.substring(genericTypeString.indexOf(",") + 1);
                String vdmMapKeyType = getVDMTypeAsStringFromJavaType(TypeName.get(getClassFromString(genericKeyType)));
                String vdmMapValueType = getVDMTypeAsStringFromJavaType(
                        TypeName.get(getClassFromString(genericValueType)));

                typeToReturn = "map " + vdmMapKeyType + " to " + vdmMapValueType;
            } else {
                throw unsupportedTypeException;
            }
        }

        return typeToReturn;
    }

    // For the java code generation super class method arguments
    public static <ProvidedType extends Value, ReturnType> ReturnType getJavaValueFromVDMValue(ProvidedType value,
            String className) throws Exception {
        String stringValue = value.toString();
        Exception unsupportedTypeException = new Exception(
                "Could not convert VDM value " + stringValue + " to Java type " + className);

        if (className.equals("byte") || className.equals("java.lang.Byte")) {
            return (ReturnType) Byte.valueOf(stringValue);
        } else if (className.equals("short") || className.equals("java.lang.Short")) {
            return (ReturnType) Short.valueOf(stringValue);
        } else if (className.equals("int") || className.equals("java.lang.Integer")) {
            return (ReturnType) Integer.valueOf(stringValue);
        } else if (className.equals("long") || className.equals("java.lang.Long")) {
            return (ReturnType) Long.valueOf(stringValue);
        } else if (className.equals("float") || className.equals("java.lang.Float")) {
            return (ReturnType) Float.valueOf(stringValue);
        } else if (className.equals("double") || className.equals("java.lang.Double")) {
            return (ReturnType) Double.valueOf(stringValue);
        } else if ((className.equals("char") || className.equals("java.lang.Character")) && !stringValue.isEmpty()) {
            return (ReturnType) Character.valueOf(stringValue.charAt(0));
        } else if (className.equals("boolean") || className.equals("java.lang.Boolean")) {
            return (ReturnType) Boolean.valueOf(stringValue);
        } else if (className.equals("java.lang.String")) {
            return (ReturnType) stringValue.substring(1, stringValue.length() - 1); // Removing quotes added by VDM
        } else if (className.contains("<")) {
            return getJavaValueWithGenericFromVDMValue(value, className, unsupportedTypeException);
        } else if (className.contains("[")) {
            return getJavaArrayValueFromVDMValue(value, className, unsupportedTypeException);
        } else {
            throw unsupportedTypeException;
        }
    }

    private static <T> String getVDMStringFromJavaMapValue(T value, Exception exception) throws Exception {
        Map<?, ?> map = (Map<?, ?>) value;
        boolean wasFirstEntryVerified = false;
        String result = "{";
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!wasFirstEntryVerified) {
                result += ", ";
            } else {
                if (!supportedJavaClasses.contains(entry.getKey().getClass())
                        || !supportedJavaClasses.contains(entry.getValue().getClass())) {
                    throw exception;
                }
                wasFirstEntryVerified = true;
            }
            result += getVDMStringFromValue(entry.getKey()) + " |-> " + getVDMStringFromValue(entry.getValue());
        }
        result += "}";
        return result;
    }

    private static <T> String getVDMStringFromJavaCollectionValue(T value, Exception exception) throws Exception {
        Collection<?> collection = (Collection<?>) value;
        boolean wasFirstEntryVerified = false;
        String result = value instanceof Set ? "{" : "[";
        Iterator<?> iterator = collection.iterator();
        while (iterator.hasNext()) {
            Object item = iterator.next();
            if (wasFirstEntryVerified) {
                result += ", ";
            } else {
                if (!supportedJavaClasses.contains(item.getClass())) {
                    throw exception;
                }
                wasFirstEntryVerified = true;
            }
            result += getVDMStringFromValue(item);
        }
        result += value instanceof Set ? "}" : "]";
        return result;
    }

    private static <T> String getVDMStringFromValue(T value) {
        if (value.getClass() == char.class || value.getClass() == Character.class) {
            return "'" + value.toString() + "'";
        } else if (value.getClass() == String.class) {
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

    private static <ProvidedType extends Value, ReturnType> ReturnType getJavaValueWithGenericFromVDMValue(
            ProvidedType value, String className, Exception exception) throws Exception {
        String classNameWithoutGeneric = className.substring(0, className.indexOf("<"));
        String genericClassName = className.substring(className.indexOf("<") + 1, className.indexOf(">"));
        Class<?> classWithoutGeneric = Class.forName(classNameWithoutGeneric);

        if (value instanceof MapValue) {
            String keyClassName = genericClassName.substring(0, className.indexOf(","));
            String valueClassName = genericClassName.substring(className.indexOf(",") + 1);
            Map<?, ?> map = (Map<?, ?>) classWithoutGeneric.getConstructor().newInstance();

            for (Entry<? extends Value, ? extends Value> mapElement : ((MapValue) value).values.entrySet()) {
                map.put(getJavaValueFromVDMValue(mapElement.getKey(), keyClassName),
                        getJavaValueFromVDMValue(mapElement.getValue(), valueClassName));
            }

            return (ReturnType) map;
        } else if (value instanceof SeqValue) {
            Collection<?> collection = new ArrayList();
            Iterator<? extends Value> iterator = (((SeqValue) value).values).iterator();

            while (iterator.hasNext()) {
                collection.add(getJavaValueFromVDMValue(iterator.next(), genericClassName));
            }

            return (ReturnType) collection;
        } else if (value instanceof Set) {
            Set<?> set = (Set<?>) new ArrayList();
            Iterator<? extends Value> iterator = (((SetValue) value).values).iterator();

            while (iterator.hasNext()) {
                set.add(getJavaValueFromVDMValue(iterator.next(), genericClassName));
            }

            return (ReturnType) set;
        } else {
            throw exception;
        }
    }

    private static <ProvidedType extends Value, ReturnType> ReturnType getJavaArrayValueFromVDMValue(ProvidedType value,
            String className, Exception exception) throws Exception {
        String arrayItemClassName = className.substring(0, className.indexOf("["));

        int arrayLength = 0;

        Iterator<? extends Value> iterator = null;
        if (value instanceof SeqValue) {
            iterator = (((SeqValue) value).values).iterator();
            arrayLength = (((SeqValue) value).values).size();
        } else if (value instanceof Set) {
            iterator = (((SetValue) value).values).iterator();
            arrayLength = (((SetValue) value).values).size();
        } else {
            throw exception;
        }

        Object array = Array.newInstance(getClassFromString(arrayItemClassName), arrayLength);

        int index = 0;
        while (iterator.hasNext()) {
            Array.set(array, index++, getJavaValueFromVDMValue(iterator.next(), arrayItemClassName));
        }

        return (ReturnType) array;
    }

    private static Class<?> getClassFromString(String text) throws Exception {
        if (text.equals("byte")) {
            return byte.class;
        } else if (text.equals("short")) {
            return short.class;
        } else if (text.equals("int")) {
            return int.class;
        } else if (text.equals("long")) {
            return long.class;
        } else if (text.equals("float")) {
            return float.class;
        } else if (text.equals("double")) {
            return double.class;
        } else if (text.equals("char")) {
            return char.class;
        } else if (text.equals("boolean")) {
            return boolean.class;
        } else {
            return Class.forName(text);
        }
    }
}
