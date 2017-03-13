/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.persistence;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.osc.core.broker.model.entities.IscEntity;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.broker.view.util.EventType;

/**
 * EntityManager: a generic entity manager that handles all common CRUD
 * operations.
 */

public class EntityManager<T extends IscEntity> {

    // private static final Logger log = Logger.getLogger(EntityManager.class);

    protected Session session;
    private Class<T> clazz;

    public EntityManager(Class<T> clazz, Session session) {
        this.session = session;
        this.clazz = clazz;
    }

    public List<T> listAll() {
        return listAll(null);
    }

    public List<T> listAll(Order[] orders) {
        Criteria criteria = this.session.createCriteria(this.clazz).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (orders != null) {
            for (Order order : orders) {
                criteria.addOrder(order);
            }
        }
        @SuppressWarnings({ "unchecked" })
        List<T> ls = criteria.list();
        return ls;
    }

    @SuppressWarnings("unchecked")
    public T findByPrimaryKey(Serializable id) {
        return (T) this.session.get(this.clazz, id);
    }

    public T create(T entity) {
        return EntityManager.create(this.session, entity);
    }

    public void update(T entity) {
        EntityManager.update(this.session, entity);
    }

    public void markDeleted(T entity) {
        EntityManager.markDeleted(this.session, entity);
    }

    public void delete(Serializable id) {
        T entity = this.findByPrimaryKey(id);
        EntityManager.delete(this.session, entity);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IscEntity> T loadPessimistically(Session session, T entity) {
        return (T) session.get(entity.getClass(), entity.getId(), new LockOptions(LockMode.PESSIMISTIC_WRITE));
    }

    public static <T extends IscEntity> void refreshPessimistically(Session session, T entity) {
        session.refresh(entity, new LockOptions(LockMode.PESSIMISTIC_WRITE));
    }

    public static <T extends IscEntity> T create(Session session, T entity) {
        String contextUser = SessionUtil.getCurrentUser();
        entity.setCreatedBy(contextUser);
        entity.setCreatedTimestamp(new Date());

        session.save(entity);

        BaseDto dto = null;
        if (entity instanceof TaskRecord) {
            dto = TaskEntityMgr.fromEntity((TaskRecord) entity);
        }

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(session, entity.getId(), entity.getClass().getSimpleName(),
                EventType.ADDED, dto);

        return entity;
    }

    public static void update(Session session, IscEntity entity) {
        String contextUser = SessionUtil.getCurrentUser();
        entity.setUpdatedBy(contextUser);
        entity.setUpdatedTimestamp(new Date());

        session.update(entity);

        BaseDto dto = null;
        if (entity instanceof TaskRecord) {
            dto = TaskEntityMgr.fromEntity((TaskRecord) entity);
        }

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(session, entity.getId(), entity.getClass().getSimpleName(),
                EventType.UPDATED, dto);
    }

    public static void markDeleted(Session session, IscEntity entity) {
        String contextUser = SessionUtil.getCurrentUser();
        entity.setMarkedForDeletion(true);
        entity.setDeletedBy(contextUser);
        entity.setDeletedTimestamp(new Date());

        session.update(entity);

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(session, entity.getId(), entity.getClass().getSimpleName(),
                EventType.UPDATED);
    }

    public static void unMarkDeleted(Session session, IscEntity entity) {
        entity.setMarkedForDeletion(false);
        entity.setDeletedBy(null);
        entity.setDeletedTimestamp(null);

        session.update(entity);

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(session, entity.getId(), entity.getClass().getSimpleName(),
                EventType.UPDATED);
    }

    public static void delete(Session session, IscEntity entity) {
        session.delete(entity);

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(session, entity.getId(), entity.getClass().getSimpleName(),
                EventType.DELETED);

        if (entity instanceof VirtualSystem) {
            // TODO: Future. Needs to be generalized broadcasting changes to UI

            // After removing a VS (not mark deleted) we send additional broadcast message to respective DA. 
            VirtualSystem vs = (VirtualSystem) entity;
            if (!vs.getDistributedAppliance().getMarkedForDeletion()) {
                TransactionalBroadcastUtil.addMessageToMap(session, vs.getDistributedAppliance().getId(), vs
                        .getDistributedAppliance().getClass().getSimpleName(), EventType.UPDATED);
            }
        }

    }

    public T findByFieldName(String entityClassFieldName, Object fieldValue) {

        Criteria criteria = this.session.createCriteria(this.clazz).add(
                Restrictions.eq(entityClassFieldName, fieldValue).ignoreCase());
        @SuppressWarnings("unchecked")
        List<T> list = criteria.setFirstResult(0).setMaxResults(1).list();

        if (list == null || list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public List<T> listByFieldName(String entityClassFieldName, Object fieldValue) {
        return listByFieldName(entityClassFieldName, fieldValue, null);
    }

    public List<T> listByFieldName(String entityClassFieldName, Object fieldValue, Order[] orders) {

        Criteria criteria = this.session.createCriteria(this.clazz)
                .add(Restrictions.eq(entityClassFieldName, fieldValue))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (orders != null) {
            for (Order order : orders) {
                criteria.addOrder(order);
            }
        }
        @SuppressWarnings("unchecked")
        List<T> list = criteria.list();

        return list;
    }

    public List<T> findByParentId(String parentEntityName, Long parentId) {
        return findByParentId(parentEntityName, parentId, null);
    }

    /**
     * Find list of children entities by their parent Id
     * 
     * @param parentEntityName
     *            The parent entity name by which id will be queried.
     * @param parentId
     *            Parent entity identifier for which children are queried.
     * @return List of children owned by parent entity
     */
    public List<T> findByParentId(String parentEntityName, Long parentId, Order[] orders) {

        Criteria criteria = this.session.createCriteria(this.clazz);
        criteria.createAlias(parentEntityName, "parent");
        criteria.add(Restrictions.eq("parent.id", parentId)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        if (orders != null) {
            for (Order order : orders) {
                criteria.addOrder(order);
            }
        }

        @SuppressWarnings("unchecked")
        List<T> list = criteria.list();

        return list;
    }

    // check the db table to see if we already have row containing the given
    // field/column value
    public boolean isExisting(String entityClassFieldName, String fieldValue) {

        Criteria criteria = this.session.createCriteria(this.clazz).add(
                Restrictions.eq(entityClassFieldName, fieldValue).ignoreCase());

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if ((count != null) && (count > 0)) {

            return true;
        }

        return false;

    }

    // check the db table to see if we already have another row that also
    // contains the same given field/column value
    public boolean isDuplicate(String entityClassFieldName, String fieldValue, long id) {

        Criteria criteria = this.session.createCriteria(this.clazz).add(Restrictions.ne("id", id))
                .add(Restrictions.eq(entityClassFieldName, fieldValue).ignoreCase());

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if ((count != null) && (count > 0)) {

            return true;
        }

        return false;

    }

    public boolean isUniqueReferenced(String entityClassFkFieldName, long fkId) {

        Criteria criteria = this.session.createCriteria(this.clazz).add(Restrictions.eq(entityClassFkFieldName, fkId));

        Long count = (Long) criteria.setProjection(Projections.rowCount()).setFirstResult(0).setMaxResults(1)
                .uniqueResult();

        if ((count != null) && (count > 0)) {

            return true;
        }

        return false;

    }
}
