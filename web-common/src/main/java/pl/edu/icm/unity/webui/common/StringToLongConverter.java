/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package pl.edu.icm.unity.webui.common;

import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.StringToIntegerConverter;


/**
 * Modified version of Vaadin's {@link StringToIntegerConverter}
 * @author K. Benedyczak
 */
public class StringToLongConverter implements Converter<String, Long>
{

	    /**
	     * Returns the format used by
	     * {@link #convertToPresentation(Long, Locale)} and
	     * {@link #convertToModel(String, Locale)}.
	     * 
	     * @param locale
	     *            The locale to use
	     * @return A NumberFormat instance
	     */
	    protected NumberFormat getFormat(Locale locale) {
	        if (locale == null) {
	            locale = Locale.getDefault();
	        }
	        return NumberFormat.getIntegerInstance(locale);
	    }

	    @Override
	    public Long convertToModel(String value, Class<? extends Long> clazz, Locale locale)
	            throws ConversionException {
	        if (value == null) {
	            return null;
	        }

	        // Remove leading and trailing white space
	        value = value.trim();

	        // Parse and detect errors. If the full string was not used, it is
	        // an error.
	        ParsePosition parsePosition = new ParsePosition(0);
	        Number parsedValue = getFormat(locale).parse(value, parsePosition);
	        if (parsePosition.getIndex() != value.length()) {
	            throw new ConversionException("Could not convert '" + value
	                    + "' to " + getModelType().getName());
	        }

	        if (parsedValue == null) {
	            // Convert "" to null
	            return null;
	        }
	        return parsedValue.longValue();
	    }

	    @Override
	    public String convertToPresentation(Long value, Class<? extends String> clazz, Locale locale)
	            throws ConversionException {
	        if (value == null) {
	            return null;
	        }

	        return getFormat(locale).format(value);
	    }

	    @Override
	    public Class<Long> getModelType() {
	        return Long.class;
	    }

	    @Override
	    public Class<String> getPresentationType() {
	        return String.class;
	    }

}
