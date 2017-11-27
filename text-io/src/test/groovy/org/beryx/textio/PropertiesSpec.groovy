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
package org.beryx.textio

import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Consumer

@Unroll
class PropertiesSpec extends Specification {
    @PropertiesPrefixes(["a1", "a2"])
    static class TermA extends AbstractTextTerminal<TermA> {
        @Override
        String read(boolean masking) { null }

        @Override
        void rawPrint(String message) {}

        @Override
        void println() {}
    }

    static class TermB extends TermA {
        TermB() {
            addDefaultProperty("key001", "B001")
            addDefaultProperty("key002", "B002")
            addDefaultProperty("key003", "B003")
        }
    }

    @PropertiesPrefixes(["c"])
    static class TermC extends TermB {}

    static class TermD extends TermC {}

    @PropertiesPrefixes(["e1", "e2", "e3"])
    static class TermE extends TermD {
        TermE() {
            addDefaultProperty("key001", "E001")
            addDefaultProperty("key002", "E002")
            addDefaultProperty("key004", "E004")
            addDefaultProperty("key005", "E005")
        }
    }

    static class TermF extends TermE {
        TermF() {
            addDefaultProperty("key002", "F002")
            addDefaultProperty("key004", "F004")
            addDefaultProperty("key006", "F006")
        }
    }

    def "should correctly retrieve the properties prefixes of #cls"() {
        given:
        TextTerminal term = cls.newInstance()

        expect:
        term.getPropertiesPrefixes() == prefixes

        where:
        cls   | prefixes
        TermA | ["textio", "a1", "a2"]
        TermB | ["textio", "a1", "a2"]
        TermC | ["textio", "a1", "a2", "c"]
        TermD | ["textio", "a1", "a2", "c"]
        TermE | ["textio", "a1", "a2", "c", "e1", "e2", "e3"]
        TermF | ["textio", "a1", "a2", "c", "e1", "e2", "e3"]
    }


    static String props1 = '''
        textio.key001 = X001
        a1.key001 = X001-a1
        a2.key001 = X001-a2
        textio.key004 = X004
        a1.key004 = X004-a1
        c.key004 = X004-c
        d.key004 = X004-d
        textio.key007 = X007
        e2.key007 = X007-e2        
    '''.stripIndent();

    static String props2 = '''
        textio.key001 = 
        c.key001 = X001-c
        e2.key001 =        
        f.key001 = X001-f         
    '''.stripIndent();

    def "should correctly initialize properties from #propsName for #cls"() {
        given:
        def propsText = PropertiesSpec."$propsName"
        TextTerminal term = cls.newInstance()
        def reader = new StringReader(propsText)
        term.initProperties(reader)
        def props = term.getProperties()

        expect:
        props.getString('key001', '???') == val001
        props.getString('key002', '???') == val002
        props.getString('key003', '???') == val003
        props.getString('key004', '???') == val004
        props.getString('key005', '???') == val005
        props.getString('key006', '???') == val006
        props.getString('key007', '???') == val007

        where:
        propsName | cls   | val001    | val002 | val003 | val004    | val005 | val006 | val007
        "props1"  | TermA | 'X001-a2' | '???'  | '???'  | 'X004-a1' | '???'  | '???'  | 'X007'
        "props1"  | TermB | 'X001-a2' | 'B002' | 'B003' | 'X004-a1' | '???'  | '???'  | 'X007'
        "props1"  | TermC | 'X001-a2' | 'B002' | 'B003' | 'X004-c'  | '???'  | '???'  | 'X007'
        "props1"  | TermD | 'X001-a2' | 'B002' | 'B003' | 'X004-c'  | '???'  | '???'  | 'X007'
        "props1"  | TermE | 'X001-a2' | 'E002' | 'B003' | 'X004-c'  | 'E005' | '???'  | 'X007-e2'
        "props1"  | TermF | 'X001-a2' | 'F002' | 'B003' | 'X004-c'  | 'E005' | 'F006' | 'X007-e2'
        "props2"  | TermA | '???'     | '???'  | '???'  | '???'     | '???'  | '???'  | '???'
        "props2"  | TermC | 'X001-c'  | 'B002' | 'B003' | '???'     | '???'  | '???'  | '???'
        "props2"  | TermE | '???'     | 'E002' | 'B003' | 'E004'    | 'E005' | '???'  | '???'
        "props2"  | TermF | '???'     | 'F002' | 'B003' | 'F004'    | 'E005' | 'F006' | '???'
    }

    def "should correctly initialize properties reading file location from system property (#location)"() {
        given:
        def userDir = System.properties['user.dir']
        if(location) {
            System.setProperty(AbstractTextTerminal.SYSPROP_PROPERTIES_FILE_LOCATION, "$userDir/src/test/resources/$location")
        }
        TextTerminal term = cls.newInstance()
        term.initProperties()
        def props = term.getProperties()

        expect:
        props.getString('key001', '???') == val001
        props.getString('key002', '???') == val002
        props.getString('key003', '???') == val003
        props.getString('key004', '???') == val004
        props.getString('key005', '???') == val005
        props.getString('key006', '???') == val006
        props.getString('key007', '???') == val007

        where:
        location              | cls   | val001     | val002 | val003 | val004 | val005 | val006 | val007
        null                  | TermA | '???'      | '???'  | '???'  | '???'  | '???'  | '???'  | '???'
        null                  | TermC | 'X001-c'   | 'B002' | 'B003' | '???'  | '???'  | '???'  | '???'
        null                  | TermE | '???'      | 'E002' | 'B003' | 'E004' | 'E005' | '???'  | '???'
        null                  | TermF | '???'      | 'F002' | 'B003' | 'F004' | 'E005' | 'F006' | '???'
        "textio-1.properties" | TermA | 'X001'     | '???'  | '???'  | '???'  | '???'  | '???'  | '???'
        "textio-1.properties" | TermC | 'X001-ccc' | 'B002' | 'B003' | '???'  | '???'  | '???'  | '???'
        "textio-1.properties" | TermE | 'X001-eee' | 'E002' | 'B003' | 'E004' | 'E005' | '???'  | '???'
        "textio-1.properties" | TermF | 'X001-eee' | 'F002' | 'B003' | 'F004' | 'E005' | 'F006' | '???'
    }
}
