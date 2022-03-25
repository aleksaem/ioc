package com.dzytsiuk.ioc.context;


import com.dzytsiuk.ioc.context.cast.JavaNumberTypeCast;
import com.dzytsiuk.ioc.entity.Bean;
import com.dzytsiuk.ioc.entity.BeanDefinition;
import com.dzytsiuk.ioc.exception.BeanInstantiationException;
import com.dzytsiuk.ioc.exception.BeanNotFoundException;
import com.dzytsiuk.ioc.exception.MultipleBeansForClassException;
import com.dzytsiuk.ioc.io.BeanDefinitionReader;
import com.dzytsiuk.ioc.io.XMLBeanDefinitionReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassPathApplicationContext implements ApplicationContext {
    private static final String SETTER_PREFIX = "set";
    private static final int SETTER_PARAMETER_INDEX = 0;

    private Map<String, Bean> beans;
    private BeanDefinitionReader beanDefinitionReader;

    public ClassPathApplicationContext() {

    }

    public ClassPathApplicationContext(String... path) {
        setBeanDefinitionReader(new XMLBeanDefinitionReader(path));
        start();
    }

    public void start() {
        beans = new HashMap<>();
        List<BeanDefinition> beanDefinitions = beanDefinitionReader.getBeanDefinitions();
        instantiateBeans(beanDefinitions);
        injectValueDependencies(beanDefinitions);
        injectRefDependencies(beanDefinitions);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        List<Bean> foundBeans = new ArrayList<>();
        for (Bean value : beans.values()) {
            if (value != null) {
                if (clazz.isAssignableFrom(value.getClass())) {
                    foundBeans.add(value);
                }
            }
        }

        if (foundBeans.size() > 1) {
            throw new MultipleBeansForClassException("Found more than one bean for class " + clazz);
        } else if (foundBeans.size() == 0) {
            throw new BeanNotFoundException("Cannot find bean for class " + clazz);
        } else {
            T bean = (T) foundBeans.get(0);
            return bean;
        }
    }

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        T bean = getBean(clazz);
        if (!beans.get(name).equals(bean)) {
            throw new BeanNotFoundException("Bean with name " + name + " cannot be found");
        }
        return bean;
    }

    @Override
    public <T> T getBean(String name) {
        Bean bean = beans.get(name);
        if (bean != null) {
            return (T) beans.get(name);
        } else {
            throw new BeanNotFoundException("Cannot find bean with name " + name);
        }
    }

    @Override
    public void setBeanDefinitionReader(BeanDefinitionReader beanDefinitionReader) {
        this.beanDefinitionReader = beanDefinitionReader;
    }

    private void instantiateBeans(List<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            instantiateBean(beanDefinition);
        }
    }

    private void instantiateBean(BeanDefinition beanDefinition) {
        try {
            Object value = Class.forName(beanDefinition.getBeanClassName())
                    .getConstructor().newInstance();
            String id = beanDefinition.getId();
            Bean bean = new Bean();
            bean.setId(id);
            bean.setValue(value);
            beans.put(id, bean);
        } catch (Exception e) {
            throw new BeanInstantiationException("Cannot create new bean because of: " + e, e);
        }
    }

    private void injectValueDependencies(List<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            Map<String, String> dependencies = beanDefinition.getDependencies();
            injectDependencies(dependencies, beanDefinition);
        }
    }

    private void injectRefDependencies(List<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            Map<String, String> dependencies = beanDefinition.getDependencies();
            injectDependencies(dependencies, beanDefinition);
        }
    }

    private void injectDependencies(Map<String, String> dependencies, BeanDefinition beanDefinition) {
        String id = beanDefinition.getId();
        Bean bean = getBean(id);
        if (bean != null) {
            if (dependencies != null) {
                for (Map.Entry<String, String> dependencyEntry : dependencies.entrySet()) {
                    try {
                        String propertyName = dependencyEntry.getKey();
                        String propertyValue = dependencyEntry.getValue();
                        String setterName = getSetterName(propertyName);
                        for (Method method : bean.getValue().getClass().getMethods()) {
                            if (method.getName().equals(setterName)) {
                                Parameter parameter = method.getParameters()[SETTER_PARAMETER_INDEX];
                                Class<?> parameterClass = parameter.getType();

                                if (parameterClass.isPrimitive()) {
                                    Object fieldValue = JavaNumberTypeCast.castPrimitive(propertyValue, parameterClass);
                                    method.invoke(bean.getValue(), fieldValue);
                                } else {
                                    method.invoke(bean.getValue(), propertyValue);
                                }
                            }
                        }
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new RuntimeException("Failed injecting dependencies because of: " + e);
                    }
                }
            }
        }

    }

    private String getSetterName(String propertyName) {
        return SETTER_PREFIX + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }
}
