/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.beryx.textio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 *
 * @author qo0p
 */
public class SetInputReader<T> extends InputReader<T, SetInputReader<T>> {

    private final Map<String, T> enumValues = new LinkedHashMap();

    public SetInputReader(Supplier<TextTerminal<?>> textTerminalSupplier, Set<T> list) {
        super(textTerminalSupplier);
        for (T value : list) {
            enumValues.put(value.toString(), value);
        }

        this.possibleValues = new ArrayList(enumValues.values());
        this.numberedPossibleValues = true;
    }

    public SetInputReader<T> withAllValues() {
        return withPossibleValues(new ArrayList(enumValues.values()));
    }

    public SetInputReader<T> withAllValuesNumbered() {
        return withNumberedPossibleValues(new ArrayList(enumValues.values()));
    }

    public SetInputReader<T> withAllValuesInline() {
        return withInlinePossibleValues(new ArrayList(enumValues.values()));
    }

    @Override
    protected InputReader.ParseResult<T> parse(String s) {
        T value = enumValues.get(s);
        if (value != null) {
            return new ParseResult<>(value);
        }
        return new ParseResult(null, getErrorMessages(s));
    }

    public static SetInputReader newInstance(TextTerminal terminal, Set list) {
        return new SetInputReader(() -> terminal, list);
    }
}
