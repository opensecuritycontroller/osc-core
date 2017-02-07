package org.osc.core.broker.util;

import org.hibernate.Session;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.rest.server.AgentAuthFilter;
import org.osc.core.broker.rest.server.NsxAuthFilter;
import org.osc.core.broker.rest.server.VmidcAuthFilter;
import org.osc.core.broker.service.persistence.EntityManager;
import org.osc.core.broker.util.db.HibernateUtil;
import org.osc.core.util.EncryptionUtil;

public class PasswordUtil {

    public static void initPasswordFromDb(String loginName) {

        Session session = HibernateUtil.getSessionFactory().openSession();
        User user = null;

        try {
            EntityManager<User> emgr = new EntityManager<User>(User.class, session);
            user = emgr.findByFieldName("loginName", loginName);
            if (user.getLoginName().equals(AgentAuthFilter.VMIDC_AGENT_LOGIN)) {
                AgentAuthFilter.VMIDC_AGENT_PASS = user.getPassword();
            } else if (user.getLoginName().equals(NsxAuthFilter.VMIDC_NSX_LOGIN)) {
                NsxAuthFilter.VMIDC_NSX_PASS = user.getPassword();
            } else if (user.getLoginName().equals(VmidcAuthFilter.VMIDC_DEFAULT_LOGIN)) {
                VmidcAuthFilter.VMIDC_DEFAULT_PASS = user.getPassword();
            }

        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
