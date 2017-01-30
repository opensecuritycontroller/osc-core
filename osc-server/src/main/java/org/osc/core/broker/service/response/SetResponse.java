package org.osc.core.broker.service.response;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Generic response object which contains a List of objects
 *
 * @param <T> the type of objects the list contains
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SetResponse<T> implements Response {

    private Set<T> set = new HashSet<T>();

    public SetResponse() {
    }

    public SetResponse(Set<T> set) {
        this.set = set;
    }

    public Set<T> getSet() {
        return this.set;
    }

    public void setSet(Set<T> set) {
        this.set = set;
    }



}
