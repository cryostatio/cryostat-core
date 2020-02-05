package com.redhat.rhjmc.containerjfr.core.reports;

import java.util.Collections;
import java.util.Map;

public interface ReportTransformer extends Comparable<ReportTransformer> {

    /**
     * CSS-style selector for elements that this transformer should apply to
     *
     * @return CSS selector
     */
    String selector();

    /**
     * Map of HTML element attributes to apply to the matched elements. Keys matching existing
     * element attributes will overwrite the attribute value. Keys not matching existing attributes
     * will cause new attributes to be added. Case-insensitive. Default implementation is an empty
     * map, which will cause no modification to the attributes.
     *
     * @return key-value mapping of HTML attributes
     */
    default Map<String, String> attributes() {
        return Collections.emptyMap();
    }

    /**
     * Function for transformation of the matched elements' inner HTML. Default implementation is a
     * no-op.
     *
     * @param innerHtml the inner HTML contents of a matched element
     * @return the transformed inner HTML contents to apply to the matched element
     */
    default String innerHtml(String innerHtml) {
        return innerHtml;
    }

    /**
     * Priority for applying this transformer. Transformers are applied in ascending order - the
     * lower the score, the earlier the application. Order is not well defined for transformers of
     * the same priority.
     */
    int priority();

    default int compareTo(ReportTransformer o) {
        return priority() - o.priority();
    }
}
