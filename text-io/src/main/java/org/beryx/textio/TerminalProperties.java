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

import java.awt.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.beryx.textio.PropertiesConstants.*;

/**
 * A map of properties associated with a TextTerminal.
 * @param <T> the type of the TextTerminal.
 */
public class TerminalProperties<T extends TextTerminal<T>> {
    private static final Logger logger =  LoggerFactory.getLogger(TerminalProperties.class);

    private final T textTerminal;

    private final Map<String, String> props = new HashMap<>();
    private final List<ExtendedChangeListener<T>> listeners = new ArrayList<>();


    public interface ChangeListener<TT extends TextTerminal<TT>, V> {
        void changed(TT textTerminal, V newVal);
    }

    @FunctionalInterface
    public interface StringChangeListener<TT extends TextTerminal<TT>> {
        void changed(TT textTerminal, String newVal);
        default ChangeListener<TT, String> get() {
            return (term, newVal) -> changed(term, newVal);
        }
    }

    @FunctionalInterface
    public interface IntChangeListener<TT extends TextTerminal<TT>> {
        void changed(TT textTerminal, Integer newVal);
        default ChangeListener<TT, Integer> get() {
            return (term, newVal) -> changed(term, newVal);
        }
    }

    @FunctionalInterface
    public interface LongChangeListener<TT extends TextTerminal<TT>> {
        void changed(TT textTerminal, Long newVal);
        default ChangeListener<TT, Long> get() {
            return (term, newVal) -> changed(term, newVal);
        }
    }

    @FunctionalInterface
    public interface DoubleChangeListener<TT extends TextTerminal<TT>> {
        void changed(TT textTerminal, Double newVal);
        default ChangeListener<TT, Double> get() {
            return (term, newVal) -> changed(term, newVal);
        }
    }

    @FunctionalInterface
    public interface BooleanChangeListener<TT extends TextTerminal<TT>> {
        void changed(TT textTerminal, Boolean newVal);
        default ChangeListener<TT, Boolean> get() {
            return (term, newVal) -> changed(term, newVal);
        }
    }

    @FunctionalInterface
    public interface ExtendedChangeListener<TT extends TextTerminal<TT>> {
        void changed(TT textTerminal, String key, String oldVal, String newVal);
    }

    private static class ChangeListenerForKey<TT extends TextTerminal<TT>, V> implements ExtendedChangeListener<TT> {
        private final String key;
        private final V defaultValue;
        private final Function<String, V> valueConverter;
        private final ChangeListener<TT, V> delegate;

        private ChangeListenerForKey(String key, V defaultValue, Function<String, V> valueConverter, ChangeListener<TT, V> delegate) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.valueConverter = valueConverter;
            this.delegate = delegate;
        }

        @Override
        public void changed(TT textTerminal, String changedKey, String sOldVal, String sNewVal) {
            if(changedKey.equals(key)) {
                V newVal = defaultValue;
                if(sNewVal != null) {
                    try {
                        newVal = valueConverter.apply(sNewVal);
                    } catch (Exception e) {
                        logger.warn("Invalid value for property " + changedKey + ": " + sNewVal, e);
                        newVal = defaultValue;
                    }
                }
                delegate.changed(textTerminal, newVal);
            }
        }
    }

    public TerminalProperties(T textTerminal) {
        this.textTerminal = textTerminal;
    }

    /**
     * @return the list of {@link ExtendedChangeListener} for this instance of TerminalProperties.
     */
    public List<ExtendedChangeListener<T>> getListeners() {
        return listeners;
    }

    /**
     * Removes the property with the specified key.
     * @param key the key whose associated value should be removed.
     * @return the old value associated with the specified key.
     */
    public Object remove(String key) {
        if(key == null) return null;
        String oldVal = props.remove(key);
        listeners.forEach(listener -> listener.changed(textTerminal, key, oldVal, null));
        return oldVal;
    }

    /**
     * Sets the value associated with the specified key.
     * @param key the key with which the specified value should be associated.
     * @param value the value to be associated with the specified key.
     * @return
     */
    public Object put(String key, Object value) {
        if(key == null) return null;
        String newVal = (value == null) ? null : String.valueOf((value));
        String oldVal = props.put(key, newVal);
        listeners.forEach(listener -> listener.changed(textTerminal, key, oldVal, newVal));
        return oldVal;
    }

    public void putAll(Map<String, ? extends Object> map) {
        if(map == null) return;
        map.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
    }

    public Set<String> getAllKeys() {
        return props.keySet();
    }

    public Set<String> getMatchingKeys(String regex) {
        return getMatchingKeys(key -> key.matches(regex));
    }

    public Set<String> getMatchingKeys(Predicate<String> keyFilter) {
        return props.keySet().stream().filter(keyFilter).collect(Collectors.toSet());
    }

    /**
     * @return the value associated with the specified key.
     */
    public String getString(String key) {
        return props.get(key);
    }

    /**
     * Gets the String value of the property with the specified key.
     * @param key the key whose associated value should be retrieved.
     * @param defaultValue the value to be returned if no value is associated with the specified key or the associated value is null or empty.
     * @return the string value of the property with the specified key.
     */
    public String getString(String key, String defaultValue) {
        String value = props.get(key);
        if(value == null || value.isEmpty()) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets the int value of the property with the specified key.
     * @param key the key whose associated value should be retrieved.
     * @param defaultValue the value to be returned if no value is associated with the specified key or the string representation of the value cannot be converted to int.
     * @return the int value of the property with the specified key.
     */
    public int getInt(String key, int defaultValue) {
        String sVal = props.get(key);
        if(sVal == null) return defaultValue;
        try {
            return Integer.parseInt(sVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the long value of the property with the specified key.
     * @param key the key whose associated value should be retrieved.
     * @param defaultValue the value to be returned if no value is associated with the specified key or the string representation of the value cannot be converted to long.
     * @return the long value of the property with the specified key.
     */
    public long getLong(String key, long defaultValue) {
        String sVal = props.get(key);
        if(sVal == null) return defaultValue;
        try {
            return Long.parseLong(sVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the double value of the property with the specified key.
     * @param key the key whose associated value should be retrieved.
     * @param defaultValue the value to be returned if no value is associated with the specified key or the string representation of the value cannot be converted to double.
     * @return the double value of the property with the specified key.
     */
    public double getDouble(String key, double defaultValue) {
        String sVal = props.get(key);
        if(sVal == null) return defaultValue;
        try {
            return Double.parseDouble(sVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the boolean value of the property with the specified key.
     * @param key the key whose associated value should be retrieved.
     * @param defaultValue the value to be returned if no value is associated with the specified key.
     * @return the boolean value of the property with the specified key.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String sVal = props.get(key);
        if(sVal == null) return defaultValue;
        return Boolean.parseBoolean(sVal);
    }

    /**
     * Adds a listener for this TerminalProperties instance.
     * @param listener the listener to be added.
     */
    public void addListener(ExtendedChangeListener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Removes the listener passed as argument.
     * @param listener the listener to be added.
     * @return true, if the listener has been found and removed.
     */
    public boolean removeListener(ExtendedChangeListener<T> listener) {
        return listeners.remove(listener);
    }


    /**
     * Convenience method that adds a listener for the String property with the specified key.
     * @param key the key of the property for which the listener is added.
     * @param defaultValue the value to be used if the new value is null.
     * @param listener the listener to be added.
     */
    public void addStringListener(String key, String defaultValue, StringChangeListener<T> listener) {
        listeners.add(new ChangeListenerForKey<>(key, defaultValue, Function.identity(), listener.get()));
    }

    /**
     * Convenience method that adds a listener for the int property with the specified key.
     * @param key the key of the property for which the listener is added.
     * @param defaultValue the value to be used if the new value cannot be converted to an int.
     * @param listener the listener to be added.
     */
    public void addIntListener(String key, int defaultValue, IntChangeListener<T> listener) {
        listeners.add(new ChangeListenerForKey<>(key, defaultValue, Integer::parseInt, listener.get()));
    }

    /**
     * Convenience method that adds a listener for the long property with the specified key.
     * @param key the key of the property for which the listener is added.
     * @param defaultValue the value to be used if the new value cannot be converted to a long.
     * @param listener the listener to be added.
     */
    public void addLongListener(String key, long defaultValue, LongChangeListener<T> listener) {
        listeners.add(new ChangeListenerForKey<>(key, defaultValue, Long::parseLong, listener.get()));
    }

    /**
     * Convenience method that adds a listener for the double property with the specified key.
     * @param key the key of the property for which the listener is added.
     * @param defaultValue the value to be used if the new value cannot be converted to a double.
     * @param listener the listener to be added.
     */
    public void addDoubleListener(String key, double defaultValue, DoubleChangeListener<T> listener) {
        listeners.add(new ChangeListenerForKey<>(key, defaultValue, Double::parseDouble, listener.get()));
    }

    /**
     * Convenience method that adds a listener for the boolean property with the specified key.
     * @param key the key of the property for which the listener is added.
     * @param defaultValue the value to be used if the new value is null.
     * @param listener the listener to be added.
     */
    public void addBooleanListener(String key, boolean defaultValue, BooleanChangeListener<T> listener) {
        listeners.add(new ChangeListenerForKey<>(key, defaultValue, Boolean::parseBoolean, listener.get()));
    }


    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /** Convenience method that associates the specified {@code color} with the key {@value PropertiesConstants#PROP_PROMPT_COLOR} */
    public void setPromptColor(String color) {
        put(PROP_PROMPT_COLOR, color);
    }

    /** Convenience method that associates the hex representation of the specified {@code color} with the key {@value PropertiesConstants#PROP_PROMPT_COLOR} */
    public void setPromptColor(Color color) {
        put(PROP_PROMPT_COLOR, toHex(color));
    }

    /** Convenience method that associates the specified {@code bgcolor} with the key {@value PropertiesConstants#PROP_PROMPT_BGCOLOR} */
    public void setPromptBackgroundColor(String bgcolor) {
        put(PROP_PROMPT_BGCOLOR, bgcolor);
    }

    /** Convenience method that associates the hex representation of the specified {@code bgcolor} with the key {@value PropertiesConstants#PROP_PROMPT_BGCOLOR} */
    public void setPromptBackgroundColor(Color bgcolor) {
        put(PROP_PROMPT_BGCOLOR, toHex(bgcolor));
    }

    /** Convenience method that associates the boolean value {@code bold} with the key {@value PropertiesConstants#PROP_PROMPT_BOLD} */
    public void setPromptBold(boolean bold) {
        put(PROP_PROMPT_BOLD, bold);
    }

    /** Convenience method that associates the boolean value {@code italic} with the key {@value PropertiesConstants#PROP_PROMPT_ITALIC} */
    public void setPromptItalic(boolean italic) {
        put(PROP_PROMPT_ITALIC, italic);
    }

    /** Convenience method that associates the boolean value {@code underline} with the key {@value PropertiesConstants#PROP_PROMPT_UNDERLINE} */
    public void setPromptUnderline(boolean underline) {
        put(PROP_PROMPT_UNDERLINE, underline);
    }

    /** Convenience method that associates the specified {@code color} with the key {@value PropertiesConstants#PROP_INPUT_COLOR} */
    public void setInputColor(String color) {
        put(PROP_INPUT_COLOR, color);
    }

    /** Convenience method that associates the hex representation of the specified {@code color} with the key {@value PropertiesConstants#PROP_INPUT_COLOR} */
    public void setInputColor(Color color) {
        put(PROP_INPUT_COLOR, toHex(color));
    }

    /** Convenience method that associates the specified {@code bgcolor} with the key {@value PropertiesConstants#PROP_INPUT_BGCOLOR} */
    public void setInputBackgroundColor(String bgcolor) {
        put(PROP_INPUT_BGCOLOR, bgcolor);
    }

    /** Convenience method that associates the hex representation of the specified {@code bgcolor} with the key {@value PropertiesConstants#PROP_INPUT_BGCOLOR} */
    public void setInputBackgroundColor(Color bgcolor) {
        put(PROP_INPUT_BGCOLOR, toHex(bgcolor));
    }

    /** Convenience method that associates the boolean value {@code bold} with the key {@value PropertiesConstants#PROP_INPUT_BOLD} */
    public void setInputBold(boolean bold) {
        put(PROP_INPUT_BOLD, bold);
    }

    /** Convenience method that associates the boolean value {@code italic} with the key {@value PropertiesConstants#PROP_INPUT_ITALIC} */
    public void setInputItalic(boolean italic) {
        put(PROP_INPUT_ITALIC, italic);
    }

    /** Convenience method that associates the boolean value {@code underline} with the key {@value PropertiesConstants#PROP_INPUT_UNDERLINE} */
    public void setInputUnderline(boolean underline) {
        put(PROP_INPUT_UNDERLINE, underline);
    }


    /** Convenience method that associates the specified {@code bgcolor} with the key {@value PropertiesConstants#PROP_PANE_BGCOLOR} */
    public void setPaneBackgroundColor(String bgcolor) {
        put(PROP_PANE_BGCOLOR, bgcolor);
    }
    /** Convenience method that associates the hex representation of the specified {@code bgcolor} with the key {@value PropertiesConstants#PROP_PANE_BGCOLOR} */
    public void setPaneBackgroundColor(Color bgcolor) {
        put(PROP_PANE_BGCOLOR, toHex(bgcolor));
    }

    /** Convenience method that associates the specified {@code width} with the key {@value PropertiesConstants#PROP_PANE_WIDTH} */
    public void setPaneWidth(int width) {
        put(PROP_PANE_WIDTH, width);
    }
    /** Convenience method that associates the specified {@code height} with the key {@value PropertiesConstants#PROP_PANE_HEIGHT} */
    public void setPaneHeight(int height) {
        put(PROP_PANE_HEIGHT, height);
    }
    /** Convenience method that associates the specified {@code width} and {@code height} with the keys {@value PropertiesConstants#PROP_PANE_WIDTH} and {@value PropertiesConstants#PROP_PANE_HEIGHT}*/
    public void setPaneDimension(int width, int height) {
        put(PROP_PANE_WIDTH, width);
        put(PROP_PANE_HEIGHT, height);
    }
}
