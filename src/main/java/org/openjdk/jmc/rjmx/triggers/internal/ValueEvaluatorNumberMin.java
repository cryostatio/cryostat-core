/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.rjmx.triggers.internal;

import java.util.logging.Logger;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.XmlToolkit;
import org.openjdk.jmc.rjmx.triggers.IValueEvaluator;
import org.w3c.dom.Element;

/**
 * Standard evaluator. Evaluates to true if the value is less than min. The value must be a Number.
 */
public final class ValueEvaluatorNumberMin implements IValueEvaluator {
	private final static Logger LOGGER = Logger.getLogger("org.openjdk.jmc.rjmx.triggers"); //$NON-NLS-1$

	private static final String XML_ELEMENT_MINVALUE = "minvalue"; //$NON-NLS-1$
	private static final String XML_ELEMENT_CONTENTTYPE = "contenttype"; //$NON-NLS-1$

	private IQuantity m_min;

	/**
	 * Constructor. Used when constructing from XML.
	 */
	public ValueEvaluatorNumberMin() {
	}

	/**
	 * Constructor.
	 *
	 * @param min
	 *            see class comment.
	 */
	public ValueEvaluatorNumberMin(IQuantity min) {
		m_min = min;
	}

	/**
	 * @see IValueEvaluator#triggerOn(Object)
	 */
	@Override
	public boolean triggerOn(Object val) throws Exception {
		if (!(val instanceof IQuantity)) {
			String logMessage = "ValueEvaluatorNumberMin: " + val + " does not have a content type set"; //$NON-NLS-1$ //$NON-NLS-2$
			LOGGER.info(logMessage);
			throw new ValueEvaluationException(logMessage,
			        "Value \"" + val.toString() + "\" is a raw value. Please set an interpretation for the attribute in the MBean Browser.");
		} else {
			return triggerOn((IQuantity) val);
		}
	}

	private boolean triggerOn(IQuantity val) throws Exception {
		if (!val.getUnit().getContentType().equals(m_min.getUnit().getContentType())) {
			String logMessage = "ValueEvaluatorNumberMin: " + val.persistableString() //$NON-NLS-1$
					+ " is not of the same content type as limit " + m_min.persistableString(); //$NON-NLS-1$
			LOGGER.info(logMessage);
			throw new ValueEvaluationException(logMessage,
			        String.format("Value \"%s\" is not of the same type as limit \"%s\"",
							val.displayUsing(IDisplayable.EXACT), m_min.displayUsing(IDisplayable.EXACT)));
		}
		boolean result = val.compareTo(m_min) < 0;
		LOGGER.info("ValueEvaluatorNumberMin: " + val.persistableString() + " < " + m_min.persistableString() + " = " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ result);
		return result;
	}

	/**
	 * @return long the value to compare with.
	 */
	public IQuantity getMin() {
		return m_min;
	}

	/**
	 * Sets the minValue (see class comment).
	 *
	 * @param minValue
	 *            the min value.
	 */
	public void setMin(IQuantity minValue) {
		m_min = minValue;
	}

	@Override
	public void initializeEvaluatorFromXml(Element node) {
		ContentType<?> contentType = UnitLookup
				.getContentType(XmlToolkit.getSetting(node, XML_ELEMENT_CONTENTTYPE, "")); //$NON-NLS-1$
		// We need to set a content type on the evaluator if we are loading an old workspace that is lacking that information.
		if (!(contentType instanceof KindOfQuantity)) {
			contentType = UnitLookup.NUMBER;
		}
		String persistedQuantity = XmlToolkit.getSetting(node, XML_ELEMENT_MINVALUE, "0"); //$NON-NLS-1$
		try {
			setMin(((KindOfQuantity<?>) contentType).parsePersisted(persistedQuantity));
		} catch (QuantityConversionException e) {
			LOGGER.warning(e.getMessage());
			setMin(((KindOfQuantity<?>) contentType).getDefaultUnit().quantity(0));
		}
	}

	@Override
	public void exportEvaluatorToXml(Element node) {
		XmlToolkit.setSetting(node, XML_ELEMENT_CONTENTTYPE, getMin().getUnit().getContentType().getIdentifier());
		XmlToolkit.setSetting(node, XML_ELEMENT_MINVALUE, getMin().persistableString());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "value < " + m_min.persistableString(); //$NON-NLS-1$
	}

	@Override
	public String getOperatorString() {
		return "<"; //$NON-NLS-1$
	}

	@Override
	public String getEvaluationConditionString() {
		return "< " + m_min.displayUsing(IDisplayable.EXACT); //$NON-NLS-1$
	}
}
