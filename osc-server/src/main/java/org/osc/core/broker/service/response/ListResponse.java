package org.osc.core.broker.service.response;

import java.util.ArrayList;
import java.util.List;

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
public class ListResponse<T> implements Response {

    private List<T> list = new ArrayList<T>();

    public ListResponse() {
    }

    public ListResponse(List<T> list) {
        this.list = list;
    }

    public List<T> getList() {
        return this.list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
