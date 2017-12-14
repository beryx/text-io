/*
 * Copyright 2016 the original author or authors.
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
package org.beryx.textio.demo.app;

import org.beryx.textio.*;
import org.beryx.textio.web.RunnerData;

import java.util.function.BiConsumer;

/**
 * A simple application illustrating the use of TextIO.
 */
public class Cuboid implements BiConsumer<TextIO, RunnerData> {
    public static void main(String[] args) {
        TextIO textIO = TextIoFactory.getTextIO();
        new Cuboid().accept(textIO, null);
    }

    @Override
    public void accept(TextIO textIO, RunnerData runnerData) {
        TextTerminal<?> terminal = textIO.getTextTerminal();
        String initData = (runnerData == null) ? null : runnerData.getInitData();
        AppUtil.printGsonMessage(terminal, initData);

        terminal.executeWithPropertiesPrefix("custom.title", t -> t.print("Cuboid dimensions: "));
        terminal.println();

        double length = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .withPropertiesPrefix("custom.length")
                .read("Length");

        double width = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .withPropertiesPrefix("custom.width")
                .read("Width");

        double height = textIO.newDoubleInputReader()
                .withMinVal(0.0)
                .withPropertiesPrefix("custom.height")
                .read("Height");


        terminal.executeWithPropertiesPrefix("custom.title",
                t -> t.print("The volume of your cuboid is: " + length * width * height));
        terminal.println();

        textIO.newStringInputReader()
                .withMinLength(0)
                .withPropertiesPrefix("custom.neutral")
                .read("\nPress enter to terminate...");
        textIO.dispose();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": computing the volume of a cuboid.\n" +
                "(Properties are dynamically changed at runtime using custom properties values.\n" +
                "Properties file: " + getClass().getSimpleName() + ".properties.)";
    }
}
