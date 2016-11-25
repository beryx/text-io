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
package org.beryx.textio

import org.beryx.textio.InputReader.ParseResult
import spock.lang.Specification
import spock.lang.Unroll

import java.awt.Point
import java.time.DayOfWeek

@Unroll
class TextIoReadSpec extends Specification {
    static { String.metaClass.stripAll = {-> TestUtil.stripAll(delegate)} }

    def terminal = new MockTerminal()
    def textIO = new TextIO(terminal)

    def "should read a long with range and default value"() {
        when:
        terminal.inputs.addAll(["aaa", "3", "17", ""])
        def millis = textIO.newLongInputReader()
                .withMinVal(5L)
                .withMaxVal(13L)
                .withPropertyName("delay")
                .withDefaultValue(10L)
                .read("Delay in milliseconds")

        then:
        terminal.output == '''
            Delay in milliseconds [10]: aaa
            Invalid value for 'delay'.
            Expected a long value between 5 and 13.
            Delay in milliseconds [10]: 3
            Invalid value for 'delay'.
            Expected a long value between 5 and 13.
            Delay in milliseconds [10]: 17
            Invalid value for 'delay'.
            Expected a long value between 5 and 13.
            Delay in milliseconds [10]:
        '''.stripAll()
        millis == 10L
        terminal.readCalls == 4
    }

    def "should read a long between #minVal and #maxVal with default value #defVal and inputs #inputs"() {
        when:
        terminal.inputs.addAll(inputs)
        def reader = textIO.newLongInputReader().withPropertyName("delay")

        if(minVal) reader.withMinVal((Long)minVal)
        if(maxVal) reader.withMaxVal((Long)maxVal)
        if(defVal) reader.withDefaultValue((Long)defVal)

        long val = reader.read("Delay in milliseconds")

        then:
        val == millis.toLong()
        terminal.readCalls == calls

        where:
        inputs                   | minVal | maxVal | defVal | millis | calls
        ["8"]                    | null   | null   | null   | 8      | 1
        ["3", "7"]               | 7      | null   | null   | 7      | 2
        ["13", "10"]             | null   | 10     | null   | 10     | 2
        ["aaa", "3", "17", ""]   | 5      | 13     | 10     | 10     | 4
        ["aaa", "3", "17", "12"] | 5      | 13     | 10     | 12     | 4
    }

    def "should throw exception when reading a long between #minVal and #maxVal with default value #defVal"() {
        when:
        terminal.inputs << ""
        def reader = textIO.newLongInputReader().withPropertyName("delay")

        if(minVal) reader.withMinVal((Long)minVal)
        if(maxVal) reader.withMaxVal((Long)maxVal)
        if(defVal) reader.withDefaultValue((Long)defVal)

        reader.read("Delay in milliseconds")

        then:
        thrown(IllegalArgumentException)

        where:
        minVal | maxVal | defVal
        7      | 4      | 5
        7      | 4      | null
        4      | 7      | 10
        null   | 7      | 10
        14     | null   | 10
    }

    def "should read a boolean without default value"() {
        when:
        terminal.inputs.addAll(["maybe", "yep", "", "false", "disabled"])
        def enabled = textIO.newBooleanInputReader()
                .withTrueInput("enabled")
                .withFalseInput("disabled")
                .withPropertyName("auto-connect")
                .read("Auto-connect feature")

        then:
        terminal.output == '''
            Auto-connect feature (enabled/disabled): maybe
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled): yep
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled):
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled): false
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled): disabled
        '''.stripAll()
        terminal.readCalls == 5
        enabled == false
    }

    def "should read a boolean with default value"() {
        when:
        terminal.inputs.addAll(["maybe", "yep", "false", ""])
        def enabled = textIO.newBooleanInputReader()
                .withTrueInput("enabled")
                .withFalseInput("disabled")
                .withDefaultValue(false)
                .withPropertyName("auto-connect")
                .read("Auto-connect feature")

        then:
        terminal.output == '''
            Auto-connect feature (enabled/disabled) [disabled]: maybe
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled) [disabled]: yep
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled) [disabled]: false
            Invalid value for 'auto-connect'.
            Expected: enabled / disabled
            Auto-connect feature (enabled/disabled) [disabled]:
        '''.stripAll()
        terminal.readCalls == 4
        enabled == false
    }

    def "should read a string with possible values and no default value"() {
        when:
        terminal.inputs.addAll(["Jack", "7", "", "3"])
        def name = textIO.newStringInputReader()
                .withPossibleValues("Jack", "Emma", "Jane", "Bill", "Laura")
                .withPropertyName("opponent")
                .read("Choose your opponent")

        then:
        terminal.output == '''
            Choose your opponent:
              1: Jack
              2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice: Jack
            Invalid value for 'opponent'. Enter a value between 1 and 5.
            Choose your opponent:
              1: Jack
              2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice: 7
            Invalid value for 'opponent'. Enter a value between 1 and 5.
            Choose your opponent:
              1: Jack
              2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice:
            Invalid value for 'opponent'. Enter a value between 1 and 5.
            Choose your opponent:
              1: Jack
              2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice: 3
        '''.stripAll()
        terminal.readCalls == 4
        name == 'Jane'
    }


    def "should read a string with possible values and default value"() {
        when:
        terminal.inputs.addAll(["Jack", "7", ""])
        def name = textIO.newStringInputReader()
                .withDefaultValue("Emma")
                .withPossibleValues("Jack", "Emma", "Jane", "Bill", "Laura")
                .withPropertyName("opponent")
                .read("Choose your opponent")

        then:
        terminal.output == '''
            Choose your opponent:
              1: Jack
            * 2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice: Jack
            Invalid value for 'opponent'. Enter a value between 1 and 5.
            Choose your opponent:
              1: Jack
            * 2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice: 7
            Invalid value for 'opponent'. Enter a value between 1 and 5.
            Choose your opponent:
              1: Jack
            * 2: Emma
              3: Jane
              4: Bill
              5: Laura
            Enter your choice:
        '''.stripAll()
        terminal.readCalls == 3
        name == 'Emma'
    }


    def "should read a string with non-numbered possible values and default value"() {
        when:
        terminal.inputs.addAll(["1", "Billy", "Jane"])
        def name = textIO.newStringInputReader()
                .withDefaultValue("Emma")
                .withPossibleValues("Jack", "Emma", "Jane", "Bill", "Laura")
                .withNumberedPossibleValues(false)
                .withPropertyName("opponent")
                .read("Choose your opponent")

        then:
        terminal.output == '''
            Choose your opponent:
              Jack
            * Emma
              Jane
              Bill
              Laura
            Enter your choice: 1
            Invalid value for 'opponent'. You must enter one of the displayed values.
            Choose your opponent:
              Jack
            * Emma
              Jane
              Bill
              Laura
            Enter your choice: Billy
            Invalid value for 'opponent'. You must enter one of the displayed values.
            Choose your opponent:
              Jack
            * Emma
              Jane
              Bill
              Laura
            Enter your choice: Jane
        '''.stripAll()
        terminal.readCalls == 3
        name == 'Jane'
    }


    def "should read a char without possible values and with no default value"() {
        when:
        terminal.inputs.addAll(["", "XY", "Z"])
        def chr = textIO.newCharInputReader()
                .read("Enter the first letter of your name")

        then:
        terminal.output == '''
            Enter the first letter of your name:
            Invalid value.
            Expected a single character value.
            Enter the first letter of your name: XY
            Invalid value.
            Expected a single character value.
            Enter the first letter of your name: Z
        '''.stripAll()
        terminal.readCalls == 3
        chr == 'Z'
    }

    def "should read a char with default value and non-numbered possible values"() {
        when:
        terminal.inputs.addAll(["AB", "E", ""])
        def groupId = textIO.newCharInputReader()
                .withPossibleValues('A', 'B', 'C', 'D')
                .withDefaultValue('C')
                .read("Group ID")

        then:
        terminal.output == '''
            Group ID:
              A
              B
            * C
              D
            Enter your choice: AB
            Invalid value.
            Expected a single character value.
            Group ID:
              A
              B
            * C
              D
            Enter your choice: E
            Invalid value. You must enter one of the displayed values.
            Group ID:
              A
              B
            * C
              D
            Enter your choice:
        '''.stripAll()
        terminal.readCalls == 3
        groupId == 'C'
    }


    def "should read a char with numbered possible values"() {
        when:
        terminal.inputs.addAll(["AB", "E", "C", "3"])
        def groupId = textIO.newCharInputReader()
                .withPossibleValues('A', 'B', 'C', 'D')
                .withNumberedPossibleValues(true)
                .read("Group ID")

        then:
        terminal.output == '''
            Group ID:
              1: A
              2: B
              3: C
              4: D
            Enter your choice: AB
            Invalid value. Enter a value between 1 and 4.
            Group ID:
              1: A
              2: B
              3: C
              4: D
            Enter your choice: E
            Invalid value. Enter a value between 1 and 4.
            Group ID:
              1: A
              2: B
              3: C
              4: D
            Enter your choice: C
            Invalid value. Enter a value between 1 and 4.
            Group ID:
              1: A
              2: B
              3: C
              4: D
            Enter your choice: 3
        '''.stripAll()
        terminal.readCalls == 4
        groupId == 'C'
    }


    def "should read an int with possible values"() {
        when:
        terminal.inputs.addAll(["abc", "12321", "15551"])
        def prime = textIO.newIntInputReader()
                .withPossibleValues(13331, 15551, 16661, 72227, 79997)
                .read("Choose your favorite prime")

        then:
        terminal.output == '''
            Choose your favorite prime:
              13331
              15551
              16661
              72227
              79997
            Enter your choice: abc
            Invalid value.
            Expected an integer value.
            Choose your favorite prime:
              13331
              15551
              16661
              72227
              79997
            Enter your choice: 12321
            Invalid value. You must enter one of the displayed values.
            Choose your favorite prime:
              13331
              15551
              16661
              72227
              79997
            Enter your choice: 15551
        '''.stripAll()
        terminal.readCalls == 3
        prime == 15551
    }


    def "should read an enum with numbered possible values"() {
        when:
        terminal.inputs.addAll(["Tuesday", "MONDAY", "3"])
        def day = textIO.newEnumInputReader(DayOfWeek.class)
                .withDefaultValue(DayOfWeek.FRIDAY)
                .withPropertyName("dayOfWeek")
                .read("Choose the day of week")

        then:
        terminal.output == '''
            Choose the day of week:
              1: MONDAY
              2: TUESDAY
              3: WEDNESDAY
              4: THURSDAY
            * 5: FRIDAY
              6: SATURDAY
              7: SUNDAY
            Enter your choice: Tuesday
            Invalid value for 'dayOfWeek'. Enter a value between 1 and 7.
            Choose the day of week:
              1: MONDAY
              2: TUESDAY
              3: WEDNESDAY
              4: THURSDAY
            * 5: FRIDAY
              6: SATURDAY
              7: SUNDAY
            Enter your choice: MONDAY
            Invalid value for 'dayOfWeek'. Enter a value between 1 and 7.
            Choose the day of week:
              1: MONDAY
              2: TUESDAY
              3: WEDNESDAY
              4: THURSDAY
            * 5: FRIDAY
              6: SATURDAY
              7: SUNDAY
            Enter your choice: 3
        '''.stripAll()
        terminal.readCalls == 3
        day == DayOfWeek.WEDNESDAY
    }


    def "should read an enum with non-numbered possible values"() {
        when:
        terminal.inputs.addAll(["1", "Tuesday", "TUESDAY"])
        def day = textIO.newEnumInputReader(DayOfWeek.class)
                .withDefaultValue(DayOfWeek.FRIDAY)
                .withNumberedPossibleValues(false)
                .read("Choose the day of week")

        then:
        terminal.output == '''
            Choose the day of week:
              MONDAY
              TUESDAY
              WEDNESDAY
              THURSDAY
            * FRIDAY
              SATURDAY
              SUNDAY
            Enter your choice: 1
            Invalid value.
            Choose the day of week:
              MONDAY
              TUESDAY
              WEDNESDAY
              THURSDAY
            * FRIDAY
              SATURDAY
              SUNDAY
            Enter your choice: Tuesday
            Invalid value.
            Choose the day of week:
              MONDAY
              TUESDAY
              WEDNESDAY
              THURSDAY
            * FRIDAY
              SATURDAY
              SUNDAY
            Enter your choice: TUESDAY
        '''.stripAll()
        terminal.readCalls == 3
        day == DayOfWeek.TUESDAY
    }

    def "should read an int[] with numbered possible values"() {
        when:
        terminal.inputs.addAll(["[44, 46, 47, 53]", "5", "2"])
        def seq = textIO.<int[]>newGenericInputReader(null)
                .withPossibleValues([42, 43, 45, 51, 52] as int[], [43, 44, 47, 48, 55, 62] as int[], [44, 46, 47, 53] as int[])
                .withValueFormatter{Arrays.toString(it)}
                .read("Choose your sequence")

        then:
        terminal.output == '''
            Choose your sequence:
              1: [42, 43, 45, 51, 52]
              2: [43, 44, 47, 48, 55, 62]
              3: [44, 46, 47, 53]
            Enter your choice: [44, 46, 47, 53]
            Invalid value. Enter a value between 1 and 3.
            Choose your sequence:
              1: [42, 43, 45, 51, 52]
              2: [43, 44, 47, 48, 55, 62]
              3: [44, 46, 47, 53]
            Enter your choice: 5
            Invalid value. Enter a value between 1 and 3.
            Choose your sequence:
              1: [42, 43, 45, 51, 52]
              2: [43, 44, 47, 48, 55, 62]
              3: [44, 46, 47, 53]
            Enter your choice: 2
        '''.stripAll()
        terminal.readCalls == 3
        seq == [43, 44, 47, 48, 55, 62]
    }


    def "should read an int[] with numbered possible values and default value"() {
        when:
        terminal.inputs.addAll(["[44, 46, 47, 53]", "5", ""])
        def seq = textIO.<int[]>newGenericInputReader(null)
                .withDefaultValue([43, 44, 47, 48, 55, 62] as int[])
                .withPossibleValues([42, 43, 45, 51, 52] as int[], [43, 44, 47, 48, 55, 62] as int[], [44, 46, 47, 53] as int[])
                .withEqualsFunc{a1,a2 -> Arrays.equals(a1,a2)}
                .withValueFormatter{Arrays.toString(it)}
                .read("Choose your sequence")

        then:
        terminal.output == '''
            Choose your sequence:
              1: [42, 43, 45, 51, 52]
            * 2: [43, 44, 47, 48, 55, 62]
              3: [44, 46, 47, 53]
            Enter your choice: [44, 46, 47, 53]
            Invalid value. Enter a value between 1 and 3.
            Choose your sequence:
              1: [42, 43, 45, 51, 52]
            * 2: [43, 44, 47, 48, 55, 62]
              3: [44, 46, 47, 53]
            Enter your choice: 5
            Invalid value. Enter a value between 1 and 3.
            Choose your sequence:
              1: [42, 43, 45, 51, 52]
            * 2: [43, 44, 47, 48, 55, 62]
              3: [44, 46, 47, 53]
            Enter your choice:
        '''.stripAll()
        terminal.readCalls == 3
        seq == [43, 44, 47, 48, 55, 62]
    }


    def "should read a Point with numbered possible values"() {
        when:
        terminal.inputs.addAll(["31:97", "5", "2"])
        def point = textIO.<Point>newGenericInputReader(null)
                .withValueFormatter{Point p -> ((int)p.x + ":" + (int)p.y) as String}
                .withPossibleValues(new Point(53, 28), new Point(31, 97), new Point(28, 66))
                .read("Choose your point")

        then:
        terminal.output == '''
            Choose your point:
              1: 53:28
              2: 31:97
              3: 28:66
            Enter your choice: 31:97
            Invalid value. Enter a value between 1 and 3.
            Choose your point:
              1: 53:28
              2: 31:97
              3: 28:66
            Enter your choice: 5
            Invalid value. Enter a value between 1 and 3.
            Choose your point:
              1: 53:28
              2: 31:97
              3: 28:66
            Enter your choice: 2
        '''.stripAll()
        terminal.readCalls == 3
        point == new Point(31, 97)
    }


    static ParseResult<Point> parsePoint(String s) {
        try {
            int[] val = s.split("\\s*:\\s*").collect {it.trim().toInteger()}
            if(val?.length != 2) throw new IllegalArgumentException()
            new ParseResult<>(new Point(val[0], val[1]))
        } catch (Exception e) {
            new ParseResult<int[]>(null, "Invalid point.")
        }
    }

    def "should read a Point with non-numbered possible values"() {
        when:
        terminal.inputs.addAll(["2", "25:45", "31:97"])
        def point = textIO.<Point>newGenericInputReader{parsePoint(it)}
                .withValueFormatter{Point p -> ((int)p.x + ":" + (int)p.y) as String}
                .withPossibleValues(new Point(53, 28), new Point(31, 97), new Point(28, 66))
                .withNumberedPossibleValues(false)
                .read("Choose your point")

        then:
        terminal.output == '''
            Choose your point:
              53:28
              31:97
              28:66
            Enter your choice: 2
            Invalid point.
            Choose your point:
              53:28
              31:97
              28:66
            Enter your choice: 25:45
            Invalid value. You must enter one of the displayed values.
            Choose your point:
              53:28
              31:97
              28:66
            Enter your choice: 31:97
        '''.stripAll()
        terminal.readCalls == 3
        point == new Point(31, 97)
    }


    def "should read a Point with no list of possible values"() {
        when:
        terminal.inputs.addAll(["2", "25:45:13", "31:97"])
        def point = textIO.<Point>newGenericInputReader{parsePoint(it)}
                .withValueFormatter{Point p -> ((int)p.x + ":" + (int)p.y) as String}
                .read("Choose your point")

        then:
        terminal.output == '''
            Choose your point: 2
            Invalid point.
            Choose your point: 25:45:13
            Invalid point.
            Choose your point: 31:97
        '''.stripAll()
        terminal.readCalls == 3
        point == new Point(31, 97)
    }
}
