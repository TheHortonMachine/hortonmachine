/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

/*
 * BeanBuilder.java
 *
 * Created on March 17, 2007, 10:27 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
import java.beans.BeanInfo;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jclarke
 */
class BeanBuilder {

    HashMap<String, PropertyDescriptor> propertyMap;
    Class beanClass;

    BeanBuilder(Class beanClass) throws IntrospectionException {
        this.beanClass = beanClass;
        loadBean();
    }

    protected void loadBean() throws IntrospectionException {
        propertyMap = new HashMap<String, PropertyDescriptor>();
        BeanInfo info = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] properties = info.getPropertyDescriptors();
        for (int i = 0; i < properties.length; i++) {
            propertyMap.put(properties[i].getName(), properties[i]);
        }
    }

    public boolean isIndexedProperty(String property) {
        return propertyMap.get(property) instanceof IndexedPropertyDescriptor;
    }

    public boolean hasProperty(String property) {
        return propertyMap.containsKey(property);
    }

    public void setProperties(Object instance, Map map) throws IllegalAccessException, InvocationTargetException {
        if (map == null) {
            return;
        }
        for (Object object : map.keySet()) {
            String property = (String) object;
            Object v = map.get(property);
            if (isIndexedProperty(property)) {
                Map m = (Map) v;
                for (Object object1 : m.keySet()) {
                    Number n = (Number) object1;
                    Object propVal = m.get(n);
//                    System.out.println("Setting Indexed Property: " + property + ": " + n + " = " + propVal);
                    setProperty(instance, property, n, propVal);
                }
            } else {
                setProperty(instance, property, v);
            }
        }
    }

    public void setProperty(Object instance, String property, Number index, Object value)
            throws IllegalAccessException, InvocationTargetException {
        setProperty(instance, property, index.intValue(), value);
    }

    public void setProperty(Object instance, String property, int index, Object value)
            throws IllegalAccessException, InvocationTargetException {
        IndexedPropertyDescriptor desc = (IndexedPropertyDescriptor) propertyMap.get(property);
        if (desc == null) {
            throw new IllegalArgumentException("Invalid property '" + property + "'");
        }
        Method meth = desc.getIndexedWriteMethod();
        Class propertyType = desc.getIndexedPropertyType();
        Object arg = getPropertyTypeValue(propertyType, value);
//        System.out.println("Invoking: " + meth.getName() + " Index = " + index + " arg = " + arg);
        meth.invoke(instance, new Object[]{new Integer(index), arg});
    }

    public void setProperty(Object instance, String property, Object value) throws IllegalAccessException, InvocationTargetException {

        PropertyDescriptor desc = propertyMap.get(property);
        if (desc == null) {
            throw new IllegalArgumentException("Invalid property '" + property + "'");
        }
        Method meth = desc.getWriteMethod();
        Class propertyType = desc.getPropertyType();
        if (propertyType == null) {
//            System.out.println("Cannot determine type for property: " + property);
            meth.invoke(instance, value);
            return;
        }
        Object arg = getPropertyTypeValue(propertyType, value);
        meth.invoke(instance, new Object[]{arg});
    }

    /**
     * converts ths incoming value to an object for the property type.
     */
    private Object getPropertyTypeValue(Class propertyType, Object value) {
        if (propertyType.equals(String.class)) {
            return value.toString();
        } else if (propertyType.equals(Boolean.class) || propertyType.equals(Boolean.TYPE)) {
            Boolean arg = null;
            if (value instanceof Boolean) {
                arg = (Boolean) value;
            } else {
                arg = Boolean.parseBoolean(value.toString());
            }
            return arg;
        } else if (propertyType.equals(Short.class) || propertyType.equals(Short.TYPE)) {
            Short arg = null;
            if (value instanceof Short) {
                arg = (Short) value;
            } else {
                arg = Short.parseShort(value.toString());
            }
            return arg;
        } else if (propertyType.equals(Integer.class) || propertyType.equals(Integer.TYPE)) {
            Integer arg = null;
            if (value instanceof Integer) {
                arg = (Integer) value;
            } else {
                arg = Integer.parseInt(value.toString());
            }
            return arg;
        } else if (propertyType.equals(Long.class) || propertyType.equals(Long.TYPE)) {
            Long arg = null;
            if (value instanceof Long) {
                arg = (Long) value;
            } else {
                arg = Long.parseLong(value.toString());
            }
            return arg;
        } else if (propertyType.equals(Float.class) || propertyType.equals(Float.TYPE)) {
            Float arg = null;
            if (value instanceof Float) {
                arg = (Float) value;
            } else {
                arg = Float.parseFloat(value.toString());
            }
            return arg;
        } else if (propertyType.equals(Double.class) || propertyType.equals(Double.TYPE)) {
            Double arg = null;
            if (value instanceof Double) {
                arg = (Double) value;
            } else {
                arg = Double.parseDouble(value.toString());
            }
            return arg;
        } else { // Object
            return value;
        }
    }
}
