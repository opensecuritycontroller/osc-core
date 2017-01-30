package org.osc.core.broker.rest.client.nsx.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "basicinfolist")
@XmlAccessorType(XmlAccessType.FIELD)
public class BasicInfoList {

    @XmlElement(name = "basicinfo")
    public List<BasicInfo> list = new ArrayList<BasicInfo>();

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (BasicInfo st : list) {
            sb.append(st.toString() + "\n");
        }
        return sb.toString();
    }

}
