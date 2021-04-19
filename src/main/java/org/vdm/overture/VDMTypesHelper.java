package org.vdm.overture;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.TypeName;

import org.overture.interpreter.values.*;

@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
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
        } else if (Enum.class.isAssignableFrom(valuesClass)) {
            return "\"" + ((Enum<?>) value).name() + "\"";
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
            return getVDMValueFromJavaValue(IntStream.range(0, Array.getLength(value))
                    .mapToObj(i -> Array.get(value, i)).collect(Collectors.toList()));
        } else if (Enum.class.isAssignableFrom(providedType)) {
            return getVDMValueFromJavaValue(((Enum<?>) value).name());
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
            } else if (Enum.class.isAssignableFrom(providedClassType)) {
                classToReturn = SeqValue.class;
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
        } else if (typeString.contains("<")) {
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
        } else {
            Class<?> providedClassType = Class.forName(typeString);
            if (Enum.class.isAssignableFrom(providedClassType)) {
                typeToReturn = "seq of char";
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

        if (className.contains("<")) {
            return getJavaValueWithGenericFromVDMValue(value, className, unsupportedTypeException);
        } else if (className.contains("[")) {
            return getJavaArrayValueFromVDMValue(value, className, unsupportedTypeException);
        } else {
            return getJavaValueFromVDMString(stringValue, className);
        }
    }

    // For the VDM interpreter return value used in aspect
    public static <ReturnType> ReturnType getJavaValueFromVDMString(String value, String className) throws Exception {
        Exception unsupportedTypeException = new Exception(
                "Could not convert VDM string " + value + " to Java type " + className);

        if (className.equals("byte") || className.equals("java.lang.Byte")) {
            return (ReturnType) Byte.valueOf(value);
        } else if (className.equals("short") || className.equals("java.lang.Short")) {
            return (ReturnType) Short.valueOf(value);
        } else if (className.equals("int") || className.equals("java.lang.Integer")) {
            return (ReturnType) Integer.valueOf(value);
        } else if (className.equals("long") || className.equals("java.lang.Long")) {
            return (ReturnType) Long.valueOf(value);
        } else if (className.equals("float") || className.equals("java.lang.Float")) {
            return (ReturnType) Float.valueOf(value);
        } else if (className.equals("double") || className.equals("java.lang.Double")) {
            return (ReturnType) Double.valueOf(value);
        } else if ((className.equals("char") || className.equals("java.lang.Character")) && !value.isEmpty()) {
            return (ReturnType) Character.valueOf(value.charAt(0));
        } else if (className.equals("boolean") || className.equals("java.lang.Boolean")) {
            return (ReturnType) Boolean.valueOf(value);
        } else if (className.equals("java.lang.String")) {
            return (ReturnType) value.substring(1, value.length() - 1); // Removing quotes added by VDM
        } else if (className.contains("<")) {
            return getJavaValueWithGenericFromVDMString(value, className, unsupportedTypeException);
        } else if (className.contains("[")) {
            return getJavaArrayValueFromVDMString(value, className, unsupportedTypeException);
        } else {
            Class providedClass = getClassFromString(className);
            if (Enum.class.isAssignableFrom(providedClass)) {
                return (ReturnType) getEnumValueFromString(providedClass, value.substring(1, value.length() - 1));
            }
            throw unsupportedTypeException;
        }
    }

    private static <T extends Enum<T>> T getEnumValueFromString(Class<T> enumClass, String value) {
        return T.valueOf(enumClass, value);
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
            String returnString = "";
            for (int i = 0; i < Array.getLength(value); i++) {
                returnString += Array.get(value, i);
            }
            return "\"" + returnString + "\"";
        } else {
            String returnString = "[";
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i != 0) {
                    returnString += ",";
                }
                returnString += Array.get(value, i);
            }
            returnString += "]";
            return returnString;
        }
    }

    private static <ProvidedType extends Value, ReturnType> ReturnType getJavaValueWithGenericFromVDMValue(
            ProvidedType value, String className, Exception exception) throws Exception {
        String classNameWithoutGeneric = className.substring(0, className.indexOf("<"));
        String genericClassName = className.substring(className.indexOf("<") + 1, className.indexOf(">"));
        Class<?> classWithoutGeneric = Class.forName(classNameWithoutGeneric);
        int classWithoutGenericModifiers = classWithoutGeneric.getModifiers();

        if (value instanceof MapValue) {
            String keyClassName = genericClassName.substring(0, className.indexOf(","));
            String valueClassName = genericClassName.substring(className.indexOf(",") + 1);
            Map<Object, Object> map;

            if (Modifier.isAbstract(classWithoutGenericModifiers)
                    || Modifier.isInterface(classWithoutGenericModifiers)) {
                map = new HashMap();
            } else {
                map = (Map<Object, Object>) classWithoutGeneric.getConstructor().newInstance();
            }

            for (Entry<? extends Value, ? extends Value> mapElement : ((MapValue) value).values.entrySet()) {
                map.put(getJavaValueFromVDMValue(mapElement.getKey(), keyClassName),
                        getJavaValueFromVDMValue(mapElement.getValue(), valueClassName));
            }

            return (ReturnType) map;
        } else if (value instanceof SeqValue) {
            Collection<Object> collection;

            if (Modifier.isAbstract(classWithoutGenericModifiers)
                    || Modifier.isInterface(classWithoutGenericModifiers)) {
                collection = new ArrayList();
            } else {
                collection = (Collection<Object>) classWithoutGeneric.getConstructor().newInstance();
            }

            Iterator<? extends Value> iterator = (((SeqValue) value).values).iterator();

            while (iterator.hasNext()) {
                collection.add(getJavaValueFromVDMValue(iterator.next(), genericClassName));
            }

            return (ReturnType) collection;
        } else if (value instanceof Set) {
            Set<Object> set;

            if (Modifier.isAbstract(classWithoutGenericModifiers)
                    || Modifier.isInterface(classWithoutGenericModifiers)) {
                set = new HashSet();
            } else {
                set = (Set<Object>) classWithoutGeneric.getConstructor().newInstance();
            }

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

    private static <ReturnType> ReturnType getJavaArrayValueFromVDMString(String value, String className,
            Exception exception) throws Exception {
        String arrayItemClassName = className.substring(0, className.indexOf("["));

        String[] array = new String[0];
        if ((value.indexOf("[") == 0 && value.lastIndexOf("]") == value.length() - 1)
                || (value.indexOf("{") == 0 && value.lastIndexOf("}") == value.length() - 1)) {
            array = value.substring(1, value.length() - 1).replaceAll("\\s", "").split(",");
        } else {
            throw exception;
        }

        Object returnArray = Array.newInstance(getClassFromString(arrayItemClassName), array.length);
        int index = 0;
        for (String element : array) {
            Array.set(returnArray, index++, getJavaValueFromVDMString(element, arrayItemClassName));
        }

        return (ReturnType) returnArray;
    }

    private static <ReturnType> ReturnType getJavaValueWithGenericFromVDMString(String value, String className,
            Exception exception) throws Exception {
        String classNameWithoutGeneric = className.substring(0, className.indexOf("<"));
        String genericClassName = className.substring(className.indexOf("<") + 1, className.indexOf(">"));
        Class<?> classWithoutGeneric = Class.forName(classNameWithoutGeneric);
        int classWithoutGenericModifiers = classWithoutGeneric.getModifiers();

        String[] array = new String[0];
        if ((value.indexOf("[") == 0 && value.lastIndexOf("]") == value.length() - 1)
                || (value.indexOf("{") == 0 && value.lastIndexOf("}") == value.length() - 1)) {
            array = value.substring(1, value.length() - 1).replaceAll("\\s", "").split(",");
        } else {
            throw exception;
        }

        if (value.indexOf("[") == 0) {
            Collection<Object> collection;

            if (Modifier.isAbstract(classWithoutGenericModifiers)
                    || Modifier.isInterface(classWithoutGenericModifiers)) {
                collection = new ArrayList();
            } else {
                collection = (Collection<Object>) classWithoutGeneric.getConstructor().newInstance();
            }

            for (String element : array) {
                collection.add(getJavaValueFromVDMString(element, genericClassName));
            }

            return (ReturnType) collection;
        } else {
            if (Map.class.isAssignableFrom(classWithoutGeneric)) {
                String keyClassName = genericClassName.substring(0, className.indexOf(","));
                String valueClassName = genericClassName.substring(className.indexOf(",") + 1);
                Map<Object, Object> map;

                if (Modifier.isAbstract(classWithoutGenericModifiers)
                        || Modifier.isInterface(classWithoutGenericModifiers)) {
                    map = new HashMap();
                } else {
                    map = (Map<Object, Object>) classWithoutGeneric.getConstructor().newInstance();
                }

                for (String element : array) {
                    int indexOfVDMMapSymbol = element.indexOf("|->");

                    if (indexOfVDMMapSymbol == -1) {
                        throw exception;
                    } else if (indexOfVDMMapSymbol != element.lastIndexOf("|->")) {
                        throw new Exception("Can not convert VDM string " + value
                                + " to Java value because it contains a complex map.");
                    } else {
                        map.put(getJavaValueFromVDMString(element.substring(0, indexOfVDMMapSymbol), keyClassName),
                                getJavaValueFromVDMString(element.substring(indexOfVDMMapSymbol + 1), valueClassName));
                    }
                }

                return (ReturnType) map;
            } else {
                Set<Object> set;

                if (Modifier.isAbstract(classWithoutGenericModifiers)
                        || Modifier.isInterface(classWithoutGenericModifiers)) {
                    set = new HashSet();
                } else {
                    set = (Set<Object>) classWithoutGeneric.getConstructor().newInstance();
                }

                for (String element : array) {
                    set.add(getJavaValueFromVDMString(element, genericClassName));
                }

                return (ReturnType) set;
            }
        }

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
