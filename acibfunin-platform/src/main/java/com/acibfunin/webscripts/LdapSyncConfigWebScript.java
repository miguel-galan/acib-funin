package com.acibfunin.webscripts;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.alfresco.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class LdapSyncConfigWebScript extends DeclarativeWebScript implements ApplicationContextAware
 {
    private static final String MASKED_VALUE = "********";
    private static final Logger log = LoggerFactory.getLogger(LdapSyncConfigWebScript.class);

    private ServiceRegistry serviceRegistry;
    private ConfigurableEnvironment environment;
    private ApplicationContext applicationContext;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        String currentUser = getCurrentUser();
        validateAdminUser(currentUser);
        Map<String, String> properties = collectAllProperties();
        Map<String, Object> model = new HashMap<>();
        model.put("success", true);
        model.put("count", properties.size());
        model.put("properties", properties);
        return model;
    }

    private String getCurrentUser() {
        return serviceRegistry.getAuthenticationService().getCurrentUserName();
    }

    private void validateAdminUser(String currentUser) {
        if (!serviceRegistry.getAuthorityService().isAdminAuthority(currentUser)) {
            String message = "User %s is not an administrator. Only administrators can view LDAP settings."
                .formatted(currentUser);
            throw new WebScriptException(Status.STATUS_FORBIDDEN, message);
        }
    }

    private Map<String, String> collectAllProperties() {
        Map<String, String> properties = new TreeMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            addSourceProperties(properties, source);
        }
        addGlobalProperties(properties);
        addSystemProperties(properties);
        return properties;
    }

    private void addSourceProperties(Map<String, String> properties, PropertySource<?> source) {
        if (!(source instanceof EnumerablePropertySource<?> enumerable)) {
            return;
        }
        for (String propertyName : enumerable.getPropertyNames()) {
            addProperty(properties, propertyName, environment.getProperty(propertyName, ""));
        }
    }

    private void addGlobalProperties(Map<String, String> properties) {
        if (applicationContext == null || !applicationContext.containsBean("global-properties")) {
            return;
        }
        Object bean = applicationContext.getBean("global-properties");
        if (!(bean instanceof Properties globalProperties)) {
            return;
        }
        for (String propertyName : globalProperties.stringPropertyNames()) {
            addProperty(properties, propertyName, globalProperties.getProperty(propertyName));
        }
    }

    private void addSystemProperties(Map<String, String> properties) {
        Properties systemProperties = System.getProperties();
        for (String propertyName : systemProperties.stringPropertyNames()) {
            addProperty(properties, propertyName, systemProperties.getProperty(propertyName));
        }
    }

    private void addProperty(Map<String, String> properties, String propertyName, String value) {
        if (isRelevantProperty(propertyName) && !properties.containsKey(propertyName)) {
            properties.put(propertyName, maskIfSensitive(propertyName, value));
        }
    }

    private boolean isRelevantProperty(String propertyName) {
        String normalized = propertyName.toLowerCase(Locale.ROOT);
        return normalized.startsWith("ldap.")
            || normalized.startsWith("synchronization.")
            || normalized.equals("authentication.chain");
    }

    private String maskIfSensitive(String propertyName, String value) {
        String normalized = propertyName.toLowerCase(Locale.ROOT);
        if (normalized.contains("password") || normalized.contains("secret")) {
            return MASKED_VALUE;
        }
        return value == null ? "" : value;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
 }