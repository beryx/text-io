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

import spock.lang.Unroll

import java.time.Month

@Unroll
class TextIoReadListSpec extends TextIoSpec {
    def "should read a list of int values with range"() {
        when:
        terminal.inputs.addAll(["", "10, aaa", "10, 3, 8", "5, 10, 20"])
        def coins = textIO.newIntInputReader()
                .withValueListChecker(InputReader.nonEmptyListChecker())
                .withMinVal(5)
                .withMaxVal(25)
                .readList("Accepted coins")

        then:
        terminal.output == '''
            Accepted coins:
            Invalid value in the comma-separated list.
            Expected at least one element.
            Accepted coins: 10, aaa
            Invalid value in the comma-separated list: aaa.
            Expected an integer value between 5 and 25.
            Accepted coins: 10, 3, 8
            Invalid value in the comma-separated list: 3.
            Expected an integer value between 5 and 25.
            Accepted coins: 5, 10, 20
        '''.stripAll()
        coins == [5, 10, 20]
        terminal.readCalls == 4
    }

    def "should read a list of integers with non-numbered possible values"() {
        when:
        terminal.inputs.addAll(["", "10, aaa", "10, 3, 8", "5, 10, 5, 20", "5, 10, 20"])
        def coins = textIO.newIntInputReader()
                .withPossibleValues(1, 2, 5, 10, 20, 50)
                .withValueListChecker(InputReader.nonEmptyListChecker())
                .withValueListChecker(InputReader.noDuplicatesChecker())
                .readList("Accepted coins")

        then:
        terminal.output == '''
            Accepted coins:
              1
              2
              5
              10
              20
              50
            Enter your choices as comma-separated values:
            Invalid value in the comma-separated list.
            Expected at least one element.
            Accepted coins:
              1
              2
              5
              10
              20
              50
            Enter your choices as comma-separated values: 10, aaa
            Invalid value in the comma-separated list: aaa.
            Expected an integer value.
            Accepted coins:
              1
              2
              5
              10
              20
              50
            Enter your choices as comma-separated values: 10, 3, 8
            Invalid value in the comma-separated list: 3. You must enter one of the displayed values.
            Accepted coins:
              1
              2
              5
              10
              20
              50
            Enter your choices as comma-separated values: 5, 10, 5, 20
            Invalid value in the comma-separated list.
            Duplicate values are not allowed.
            Accepted coins:
              1
              2
              5
              10
              20
              50
            Enter your choices as comma-separated values: 5, 10, 20
        '''.stripAll()
        coins == [5, 10, 20]
        terminal.readCalls == 5
    }


    def "should read a list of months"() {
        when:
        terminal.inputs.addAll(["", "10, MAY", "10, 13, 8", "5, 10, 5, 2", "5, 10, 2"])
        def months = textIO.newEnumInputReader(Month.class)
                .withValueListChecker(InputReader.nonEmptyListChecker())
                .withValueListChecker(InputReader.noDuplicatesChecker())
                .readList("Favorite months")

        then:
        terminal.output == '''
            Favorite months:
              1: JANUARY
              2: FEBRUARY
              3: MARCH
              4: APRIL
              5: MAY
              6: JUNE
              7: JULY
              8: AUGUST
              9: SEPTEMBER
              10: OCTOBER
              11: NOVEMBER
              12: DECEMBER
            Enter your choices as comma-separated values:
            Invalid value in the comma-separated list.
            Expected at least one element.
            Favorite months:
              1: JANUARY
              2: FEBRUARY
              3: MARCH
              4: APRIL
              5: MAY
              6: JUNE
              7: JULY
              8: AUGUST
              9: SEPTEMBER
              10: OCTOBER
              11: NOVEMBER
              12: DECEMBER
            Enter your choices as comma-separated values: 10, MAY
            Invalid value in the comma-separated list: MAY. Enter a value between 1 and 12.
            Favorite months:
              1: JANUARY
              2: FEBRUARY
              3: MARCH
              4: APRIL
              5: MAY
              6: JUNE
              7: JULY
              8: AUGUST
              9: SEPTEMBER
              10: OCTOBER
              11: NOVEMBER
              12: DECEMBER
            Enter your choices as comma-separated values: 10, 13, 8
            Invalid value in the comma-separated list: 13. Enter a value between 1 and 12.
            Favorite months:
              1: JANUARY
              2: FEBRUARY
              3: MARCH
              4: APRIL
              5: MAY
              6: JUNE
              7: JULY
              8: AUGUST
              9: SEPTEMBER
              10: OCTOBER
              11: NOVEMBER
              12: DECEMBER
            Enter your choices as comma-separated values: 5, 10, 5, 2
            Invalid value in the comma-separated list.
            Duplicate values are not allowed.
            Favorite months:
              1: JANUARY
              2: FEBRUARY
              3: MARCH
              4: APRIL
              5: MAY
              6: JUNE
              7: JULY
              8: AUGUST
              9: SEPTEMBER
              10: OCTOBER
              11: NOVEMBER
              12: DECEMBER
            Enter your choices as comma-separated values: 5, 10, 2
        '''.stripAll()
        months == [Month.MAY, Month.OCTOBER, Month.FEBRUARY]
        terminal.readCalls == 5
    }

}
