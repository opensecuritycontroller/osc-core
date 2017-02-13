package org.osc.core.broker.util;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.OscAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.db.HibernateUtil;

public class PasswordUtil {

    public static void initPasswordFromDb(String loginName) {

        Session session = HibernateUtil.getSessionFactory().openSession();
        User user;

        try {
            EntityManager<User> emgr = new EntityManager<User>(User.class, session);
            user = emgr.findByFieldName("loginName", loginName);
            if (user.getLoginName().equals(AgentAuthFilter.VMIDC_AGENT_LOGIN)) {
                AgentAuthFilter.VMIDC_AGENT_PASS = user.getPassword();
            } else if (user.getLoginName().equals(NsxAuthFilter.VMIDC_NSX_LOGIN)) {
                NsxAuthFilter.VMIDC_NSX_PASS = user.getPassword();
            } else if (user.getLoginName().equals(OscAuthFilter.OSC_DEFAULT_LOGIN)) {
                OscAuthFilter.OSC_DEFAULT_PASS = user.getPassword();
            }

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
