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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.osc.core.broker.model.entities.IscEntity;
import org.osc.core.broker.model.entities.User;
import org.osc.core.broker.model.entities.appliance.VirtualSystem;
import org.osc.core.broker.model.entities.job.TaskRecord;
import org.osc.core.broker.service.broadcast.EventType;
import org.osc.core.broker.service.dto.BaseDto;
import org.osc.core.broker.service.dto.UserDto;
import org.osc.core.broker.util.SessionUtil;
import org.osc.core.broker.util.TransactionalBroadcastUtil;
import org.osc.core.util.encryption.EncryptionException;

/**
 * EntityManager: a generic entity manager that handles all common CRUD
 * operations.
 */

public class OSCEntityManager<T extends IscEntity> {

    private static final Logger log = Logger.getLogger(OSCEntityManager.class);

    protected EntityManager em;
    private Class<T> clazz;

    public OSCEntityManager(Class<T> clazz, EntityManager em) {
        this.em = em;
        this.clazz = clazz;
    }

    public List<T> listAll() {
        return listAll(false);
    }

    public List<T> listAll(String... orderByAsc) {
        return listAll(true, orderByAsc);
    }

    public List<T> listAll(boolean asc, String... orderby) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);
        Root<T> from = query.from(this.clazz);
        query = query.select(from).distinct(true);

        if (orderby != null) {
            query = query.orderBy(Arrays.stream(orderby)
                    .map(f -> from.get(f))
                    .map(e -> asc ? cb.asc(e) : cb.desc(e))
                    .toArray(i -> new Order[i]));
        }
        List<T> ls = this.em.createQuery(query).getResultList();
        return ls;
    }

    public T findByPrimaryKey(Serializable id) {
        return this.em.find(this.clazz, id);
    }

    public T create(T entity) {
        return OSCEntityManager.create(this.em, entity);
    }

    public void update(T entity) {
        OSCEntityManager.update(this.em, entity);
    }

    public void markDeleted(T entity) {
        OSCEntityManager.markDeleted(this.em, entity);
    }

    public void delete(Serializable id) {
        T entity = this.findByPrimaryKey(id);
        OSCEntityManager.delete(this.em, entity);
    }

    @SuppressWarnings("unchecked")
    public static <T extends IscEntity> T loadPessimistically(EntityManager em, T entity) {
        return (T) em.find(entity.getClass(), entity.getId(), LockModeType.PESSIMISTIC_WRITE);
    }

    public static <T extends IscEntity> void refreshPessimistically(EntityManager em, T entity) {
        em.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
    }

    public static <T extends IscEntity> T create(EntityManager em, T entity) {
        String contextUser = SessionUtil.getCurrentUser();
        entity.setCreatedBy(contextUser);
        entity.setCreatedTimestamp(new Date());

        em.persist(entity);

        BaseDto dto = null;
        if (entity instanceof TaskRecord) {
            dto = TaskEntityMgr.fromEntity((TaskRecord) entity);
        }

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(entity.getId(), entity.getClass().getSimpleName(),
                EventType.ADDED, dto);

        return entity;
    }

    public static void update(EntityManager em, IscEntity entity) {
        String contextUser = SessionUtil.getCurrentUser();
        entity.setUpdatedBy(contextUser);
        entity.setUpdatedTimestamp(new Date());

        em.merge(entity);

        BaseDto dto = null;
        if (entity instanceof TaskRecord) {
            dto = TaskEntityMgr.fromEntity((TaskRecord) entity);
        }

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(entity.getId(), entity.getClass().getSimpleName(),
                EventType.UPDATED, dto);
    }

    public static void markDeleted(EntityManager em, IscEntity entity) {
        String contextUser = SessionUtil.getCurrentUser();
        entity.setMarkedForDeletion(true);
        entity.setDeletedBy(contextUser);
        entity.setDeletedTimestamp(new Date());

        em.merge(entity);

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(entity.getId(), entity.getClass().getSimpleName(),
                EventType.UPDATED);
    }

    public static void unMarkDeleted(EntityManager em, IscEntity entity) {
        entity.setMarkedForDeletion(false);
        entity.setDeletedBy(null);
        entity.setDeletedTimestamp(null);

        em.merge(entity);

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(entity.getId(), entity.getClass().getSimpleName(),
                EventType.UPDATED);
    }

    public static void delete(EntityManager em, IscEntity entity) {
        em.remove(entity);

        BaseDto dto = null;
        if (entity instanceof User) {
            dto = new UserDto();
            try {
                UserEntityMgr.fromEntity((User) entity, (UserDto) dto);
            } catch (EncryptionException e) {
                log.error("Unable to populate the user dto");
                throw new RuntimeException("Encountered an error when trying to delete a user", e);
            }
        }

        // Broadcasting changes to UI
        TransactionalBroadcastUtil.addMessageToMap(entity.getId(), entity.getClass().getSimpleName(),
                EventType.DELETED, dto);

        if (entity instanceof VirtualSystem) {
            // TODO: Future. Needs to be generalized broadcasting changes to UI

            // After removing a VS (not mark deleted) we send additional broadcast message to respective DA.
            VirtualSystem vs = (VirtualSystem) entity;
            if (!vs.getDistributedAppliance().getMarkedForDeletion()) {
                TransactionalBroadcastUtil.addMessageToMap(vs.getDistributedAppliance().getId(), vs
                        .getDistributedAppliance().getClass().getSimpleName(), EventType.UPDATED);
            }
        }

    }

    public T findByFieldName(String entityClassFieldName, String fieldValue) {

        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);

        Root<T> root = query.from(this.clazz);
        query = query.select(root).where(
                cb.equal(cb.lower(root.get(entityClassFieldName)),
                        cb.lower(cb.literal(fieldValue))));

        try {
            return this.em.createQuery(query).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public List<T> listByFieldName(String entityClassFieldName, Object fieldValue) {
        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);

        Root<T> root = query.from(this.clazz);
        query = query.select(root)
                .distinct(true)
                .where(
                        cb.equal(root.get(entityClassFieldName), fieldValue));

        List<T> list = this.em.createQuery(query).getResultList();

        return list;
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
    public List<T> findByParentId(String parentEntityName, Long parentId, String... orderby) {

        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);

        Root<T> root = query.from(this.clazz);
        query = query.select(root)
                .distinct(true)
                .where(
                cb.equal(root.join(parentEntityName).get("id"), parentId));

        if (orderby != null) {
            query = query.orderBy(Arrays.stream(orderby)
                    .map(f -> cb.asc(root.get(f)))
                    .toArray(i -> new Order[i]));
        }

        List<T> list = this.em.createQuery(query).getResultList();

        return list;
    }

    // check the db table to see if we already have row containing the given
    // field/column value
    public boolean isExisting(String entityClassFieldName, String fieldValue) {

        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);

        Root<T> root = query.from(this.clazz);

        query = query.select(root)
            .where(cb.equal(cb.lower(root.get(entityClassFieldName)),
                    cb.lower(cb.literal(fieldValue))));

        return !this.em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    // check the db table to see if we already have another row that also
    // contains the same given field/column value
    public boolean isDuplicate(String entityClassFieldName, String fieldValue, long id) {

        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);

        Root<T> root = query.from(this.clazz);

        query = query.select(root)
            .where(cb.notEqual(root.get("id"), id),
                    cb.equal(cb.lower(root.get(entityClassFieldName)),
                             cb.lower(cb.literal(fieldValue))));

        return !this.em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    public boolean isUniqueReferenced(String entityClassFkFieldName, long fkId) {

        CriteriaBuilder cb = this.em.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(this.clazz);

        Root<T> root = query.from(this.clazz);

        query = query.select(root)
            .where(cb.equal(root.get(entityClassFkFieldName), fkId));

        return !this.em.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }
}
