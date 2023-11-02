package org.beryx.textio.i18n;

import java.util.Objects;

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
public class TextIoI18nService {

    private TextIoI18nLanguageCode defaultLanguage = TextIoI18nLanguageCode.EN;

    private TextIoI18nService () { }

    private static final class Holder {
        private static final TextIoI18nService INSTANCE = new TextIoI18nService ();
    }

    public void setDefaultLanguage (TextIoI18nLanguageCode defaultLanguage) {
        Objects.requireNonNull (defaultLanguage);
        this.defaultLanguage = defaultLanguage;
    }

    public static TextIoI18nService getInstance () { return Holder.INSTANCE; }

    public TextIoI18nLanguageCode getDefaultLanguage () { return defaultLanguage; }

    public String getMessage (String messageKey, TextIoI18nLanguageCode languageCode, Object... args) {
        if (languageCode != null) {
            return Objects.requireNonNull (languageCode.getLanguageService ().getMessage (messageKey, args), defaultLanguage.getLanguageService ().getMessage (messageKey, args));
        } else {
            return defaultLanguage.getLanguageService ().getMessage (messageKey, args);
        }
    }
}
