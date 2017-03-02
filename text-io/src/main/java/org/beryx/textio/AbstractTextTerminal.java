/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.beryx.textio;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

@PropertiesPrefixes({"textio"})
public abstract class AbstractTextTerminal<T extends AbstractTextTerminal<T>> implements TextTerminal<T> {
    private static final Logger logger =  LoggerFactory.getLogger(AbstractTextTerminal.class);

    public static final String SYSPROP_PROPERTIES_FILE_LOCATION = "textio.properties.location";

    public static final String PROP_USER_INTERRUPT_KEY = "user.interrupt.key";
    public static final String DEFAULT_USER_INTERRUPT_KEY = "ctrl C";

    private final ObservableMap<String, String> properties = FXCollections.observableHashMap();
    private final Map<String, String> defaultProperties = new HashMap<>();

    private boolean initialized = false;

    public AbstractTextTerminal() {
        addDefaultProperty(PROP_USER_INTERRUPT_KEY, DEFAULT_USER_INTERRUPT_KEY);
    }

    @Override
    public ObservableMap<String, String> getProperties() {
        return properties;
    }

    @Override
    public void init() {
        if(initialized) return;
        initialized = true;
        initProperties();
    }

    public List<String> getPropertiesPrefixes() {
        return getPropertiesPrefixes(getClass());
    }

    private List<String> getPropertiesPrefixes(Class<?> cls) {
        if((cls == null) || !(TextTerminal.class.isAssignableFrom(cls))) return Collections.emptyList();
        List<String> superPrefixes = getPropertiesPrefixes(cls.getSuperclass());
        PropertiesPrefixes annotation = cls.getAnnotation(PropertiesPrefixes.class);
        if(annotation == null) return superPrefixes;
        List<String> prefixes = Arrays.asList(annotation.value());
        if(superPrefixes.isEmpty()) return prefixes;
        List<String> allPrefixes = new ArrayList<>(superPrefixes);
        allPrefixes.addAll(prefixes);
        return allPrefixes;
    }

    public final Map<String,String> getDefaultProperties() {
        return defaultProperties;
    }

    public final String addDefaultProperty(String key, String value) {
        return defaultProperties.put(key, value);
    }

    public void initProperties() {
        getPropertiesReader().ifPresent(reader -> initProperties(reader));
    }

    public Optional<Reader> getPropertiesReader() {
        String propsPath = System.getProperty(SYSPROP_PROPERTIES_FILE_LOCATION, null);
        if(propsPath != null) {
            logger.debug("Found system property " + SYSPROP_PROPERTIES_FILE_LOCATION + " with value: " + propsPath);
        } else {
            logger.debug("System property " + SYSPROP_PROPERTIES_FILE_LOCATION + " not set.");
            propsPath = System.getProperty("user.dir") + "/textio.properties";
        }
        File propsFile = new File(propsPath);
        if(propsFile.exists()) {
            try {
                Reader reader = new FileReader(propsFile);
                logger.debug("Found terminal properties file " + propsFile.getAbsolutePath());
                return Optional.of(reader);
            } catch (FileNotFoundException e) {
                logger.warn("Cannot read terminal properties from " + propsFile.getAbsolutePath(), e);
            }
        } else {
            logger.debug("Terminal properties file " + propsFile.getAbsolutePath() + " not found");
        }
        URL propsResource = getClass().getResource("/textio.properties");
        if(propsResource != null) {
            logger.debug("Found terminal properties file in classpath: " + propsResource);
            try {
                return Optional.of(new InputStreamReader(propsResource.openStream()));
            } catch (IOException e) {
                logger.warn("Cannot read terminal properties from " + propsResource, e);
            }
        } else {
            logger.debug("No terminal properties file found in classpath.");
        }
        return Optional.empty();
    }

    public void initProperties(Reader propsReader) {
        Properties rawProps = new Properties();
        try {
            rawProps.load(propsReader);
        } catch (IOException e) {
            logger.warn("Failed to read terminal properties.", e);
        }
        initProperties(rawProps);
    }

    public void initProperties(Properties rawProps) {
        Map<String,String> props = new HashMap<>(defaultProperties);
        for(String pp : getPropertiesPrefixes()) {
            String prefix = pp + ".";
            int prefixLen = prefix.length();
            for(String key : rawProps.stringPropertyNames()) {
                if(key.startsWith(prefix)) {
                    String value = rawProps.getProperty(key);
                    props.put(key.substring(prefixLen), value);
                }
            }
        }
        properties.putAll(props);
    }
}
