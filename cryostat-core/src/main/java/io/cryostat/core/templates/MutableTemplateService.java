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
package io.cryostat.core.templates;

import java.io.IOException;
import java.io.InputStream;

import io.cryostat.libcryostat.templates.InvalidEventTemplateException;
import io.cryostat.libcryostat.templates.Template;

public interface MutableTemplateService extends TemplateService {

    Template addTemplate(InputStream templateStream)
            throws InvalidXmlException, InvalidEventTemplateException, IOException;

    default void deleteTemplate(Template template)
            throws IOException, InvalidEventTemplateException {
        deleteTemplate(template.getName());
    }

    void deleteTemplate(String templateName) throws IOException, InvalidEventTemplateException;

    @SuppressWarnings("serial")
    public static class InvalidXmlException extends Exception {
        InvalidXmlException(String message, Throwable cause) {
            super(message, cause);
        }

        InvalidXmlException(String message) {
            super(message);
        }
    }
}
