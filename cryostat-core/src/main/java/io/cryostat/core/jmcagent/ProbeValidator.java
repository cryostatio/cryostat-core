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

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ProbeValidator extends Validator {
    private final Validator validator;

    private boolean validationResult = false;

    private static final String PROBE_SCHEMA_XSD = "jfrprobes_schema.xsd";
    private static final Schema PROBE_SCHEMA;

    static {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            PROBE_SCHEMA =
                    factory.newSchema(
                            new StreamSource(
                                    ProbeValidator.class.getResourceAsStream(PROBE_SCHEMA_XSD)));
        } catch (SAXException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public ProbeValidator() {
        validator = PROBE_SCHEMA.newValidator();
        validator.setErrorHandler(new ProbeValidatorErrorHandler());
    }

    @Override
    public void reset() {
        validationResult = false;
        validator.reset();
    }

    @Override
    public void validate(Source source, Result result) throws SAXException, IOException {
        validator.validate(source, result);
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        throw new UnsupportedOperationException("setErrorHandler is unsupported");
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return validator.getErrorHandler();
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        validator.setResourceResolver(resourceResolver);
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return validator.getResourceResolver();
    }

    private static class ProbeValidatorErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            throw new ProbeValidationException(exception.getMessage(), exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw new ProbeValidationException(exception.getMessage(), exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw new ProbeValidationException(exception.getMessage(), exception);
        }
    }
}
