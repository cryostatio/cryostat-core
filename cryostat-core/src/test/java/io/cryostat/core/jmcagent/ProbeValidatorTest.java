/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.core.jmcagent;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@ExtendWith(MockitoExtension.class)
public class ProbeValidatorTest {

    final String VALID_XML =
            "<jfragent>\n"
                    + //
                    "\t<events>\n"
                    + //
                    "\t\t<event id=\"demo.jfr.test1\">\n"
                    + //
                    "\t\t\t<label>JFR Hello World Event 1 %TEST_NAME%</label>\n"
                    + //
                    "\t\t\t<description>Defined in the xml file and added by the"
                    + " agent.</description>\n"
                    + //
                    "\t\t\t<path>demo/jfrhelloworldevent1</path>\n"
                    + //
                    "\t\t\t<stacktrace>true</stacktrace>\n"
                    + //
                    "\t\t\t<class>org.openjdk.jmc.agent.test.InstrumentMe</class>\n"
                    + //
                    "\t\t\t<method>\n"
                    + //
                    "\t\t\t\t<name>printHelloWorldJFR1</name>\n"
                    + //
                    "\t\t\t\t<descriptor>()V</descriptor>\n"
                    + //
                    "\t\t\t</method>\n"
                    + //
                    "\t\t\t<!-- location {ENTRY, EXIT, WRAP}-->\n"
                    + //
                    "\t\t\t<location>WRAP</location>\n"
                    + //
                    "\t\t\t<fields>\n"
                    + //
                    "\t\t\t\t<field>\n"
                    + //
                    "\t\t\t\t\t<name>'InstrumentMe.STATIC_STRING_FIELD'</name>\n"
                    + //
                    "\t\t\t\t\t<description>Capturing outer class field with class name prefixed"
                    + " field name</description>\n"
                    + //
                    "\t\t\t\t\t<expression>InstrumentMe.STATIC_STRING_FIELD</expression>\n"
                    + //
                    "\t\t\t\t</field>\n"
                    + //
                    "\t\t\t</fields>\n"
                    + //
                    "\t\t</event>\n"
                    + //
                    "\t</events>\n"
                    + //
                    "</jfragent>";

    final String INVALID_XML =
            "<jfragent>\n"
                    + //
                    "\t<foo>\n"
                    + //
                    "\t<bar>\n"
                    + //
                    "'This XML is not a valid probe template'\n"
                    + //
                    "\t</bar>\n"
                    + //
                    "\t</foo>\n"
                    + //
                    "\t</jfragent>";

    ProbeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProbeValidator();
    }

    @Test
    void shouldThrowProbeValidationErrorOnFailure() {
        JMCAgentXMLStream stream =
                new JMCAgentXMLStream(
                        new ByteArrayInputStream(INVALID_XML.getBytes(StandardCharsets.UTF_8)));
        Assertions.assertThrows(
                ProbeValidationException.class,
                () -> {
                    validator.validate(new StreamSource(stream));
                });
    }

    @Test
    void shouldSuccessfullyValidateCorrectTemplate() {
        JMCAgentXMLStream stream =
                new JMCAgentXMLStream(
                        new ByteArrayInputStream(VALID_XML.getBytes(StandardCharsets.UTF_8)));
        try {
            validator.validate(new StreamSource(stream));
        } catch (Exception e) {
            System.out.println(e.toString());
            Assertions.fail();
        }
    }

    @Test
    void shouldNotAllowOverridingErrorHandler() {
        Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    validator.setErrorHandler(new TestErrorHandler());
                });
    }

    private static class TestErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw new SAXException("We shouldn't have reached here");
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw new SAXException("We shouldn't have reached here");
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw new SAXException("We shouldn't have reached here");
        }
    }
}
