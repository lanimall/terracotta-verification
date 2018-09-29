package org.terracotta.utils.domain;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.search.attribute.AttributeExtractorException;

/**
 * Created by fabien.sanglier on 6/13/16.
 */
public class FullNameExtractor implements AttributeExtractor {

    private static final long serialVersionUID = 1L;

    @Override
    public Object attributeFor(Element element, String attributeName) throws AttributeExtractorException {
        String extracted = "";
        if(element.getObjectValue() == null)
            return extracted;

        if(element.getObjectValue() instanceof Customer){
            Customer cust = (Customer) element.getObjectValue();
            extracted = String.format("%s.%s.%s", cust.getFirstName(), cust.getMiddleName(), cust.getLastName());
        }

        return extracted;
    }
}