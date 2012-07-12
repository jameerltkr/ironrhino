package org.ironrhino.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.metadata.NaturalId;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Recordable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

public class BaseManagerImpl<T extends Persistable> implements BaseManager<T> {

	protected Logger log = LoggerFactory.getLogger(BaseManagerImpl.class);

	protected Class<T> entityClass;

	private ThreadLocal<Class<T>> entityClassHolder = new ThreadLocal<Class<T>>();

	protected SessionFactory sessionFactory;

	@Required
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public BaseManagerImpl() {
		Class clazz = ReflectionUtils.getGenericClass(getClass());
		if (clazz != null)
			entityClass = clazz;
	}

	public BaseManagerImpl(Class<T> clazz) {
		entityClass = clazz;
	}

	public void setEntityClass(Class<T> clazz) {
		entityClassHolder.set(clazz);
	}

	public Class<T> getEntityClass() {
		if (entityClass != null)
			return entityClass;
		return entityClassHolder.get();
	}

	@Transactional
	public void save(T obj) {
		Session session = sessionFactory.getCurrentSession();
		if (obj instanceof Recordable) {
			Recordable r = (Recordable) obj;
			Date date = new Date();
			if (obj.isNew()) {
				r.setCreateDate(date);
				r.setCreateUser(AuthzUtils.getUserDetails(UserDetails.class));
			} else {
				r.setModifyDate(date);
				r.setModifyUser(AuthzUtils.getUserDetails(UserDetails.class));
			}
		}
		if (obj instanceof BaseTreeableEntity) {
			final BaseTreeableEntity entity = (BaseTreeableEntity) obj;
			if (entity.isNew()) {
				FlushMode mode = session.getFlushMode();
				session.setFlushMode(FlushMode.MANUAL);
				entity.setFullId("");
				session.save(entity);
				session.flush();
				session.setFlushMode(mode);
			}
			String fullId;
			if (entity.getParent() == null)
				fullId = String.valueOf(entity.getId());
			else
				fullId = (entity.getParent()).getFullId() + "."
						+ String.valueOf(entity.getId());
			entity.setFullId(fullId);
			entity.setLevel(fullId.split("\\.").length);
		}
		session.saveOrUpdate(obj);
	}

	@Transactional
	public void delete(T obj) {
		sessionFactory.getCurrentSession().delete(obj);
	}

	@Transactional(readOnly = true)
	public boolean canDelete(T obj) {
		return true;
	}

	@Transactional(readOnly = true)
	public T get(Serializable id) {
		if (id == null)
			return null;
		return (T) sessionFactory.getCurrentSession().get(getEntityClass(), id);
	}

	public DetachedCriteria detachedCriteria() {
		return DetachedCriteria.forClass(getEntityClass());
	}

	@Transactional(readOnly = true)
	public long countByCriteria(DetachedCriteria dc) {
		Criteria c = dc.getExecutableCriteria(sessionFactory
				.getCurrentSession());
		c.setProjection(Projections.projectionList()
				.add(Projections.rowCount()));
		return (Long) c.uniqueResult();

	}

	@Transactional(readOnly = true)
	public T findByCriteria(DetachedCriteria dc) {
		Criteria c = dc.getExecutableCriteria(sessionFactory
				.getCurrentSession());
		c.setMaxResults(1);
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return (T) c.uniqueResult();
	}

	@Transactional(readOnly = true)
	public List<T> findListByCriteria(DetachedCriteria dc) {
		try {
			Criteria c = dc.getExecutableCriteria(sessionFactory
					.getCurrentSession());
			c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			return c.list();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new ArrayList<T>();
		}
	}

	@Transactional(readOnly = true)
	public List<T> findBetweenListByCriteria(DetachedCriteria dc, int start,
			int end) {
		try {
			Criteria c = dc.getExecutableCriteria(sessionFactory
					.getCurrentSession());
			if (!(start == 0 && end == Integer.MAX_VALUE)) {
				int firstResult = start;
				if (firstResult < 0)
					firstResult = 0;
				c.setFirstResult(firstResult);
				int maxResults = end - firstResult;
				if (maxResults > 0)
					c.setMaxResults(maxResults);
			}
			return c.list();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new ArrayList<T>();
		}
	}

	@Transactional(readOnly = true)
	public List<T> findListByCriteria(DetachedCriteria dc, int pageNo,
			int pageSize) {
		return findBetweenListByCriteria(dc, (pageNo - 1) * pageSize, pageNo
				* pageSize);
	}

	@Transactional(readOnly = true)
	public ResultPage<T> findByResultPage(ResultPage<T> resultPage) {
		long totalRecord = -1;
		if (resultPage.isCounting()) {
			totalRecord = countByCriteria(resultPage.getDetachedCriteria());
			resultPage.setTotalRecord(totalRecord);
			if (resultPage.getPageSize() < 1)
				resultPage.setPageSize(ResultPage.DEFAULT_PAGE_SIZE);
			else if (resultPage.getPageSize() < Integer.MAX_VALUE
					&& resultPage.getPageSize() > ResultPage.MAX_RECORDS_PER_PAGE)
				resultPage.setPageSize(ResultPage.MAX_RECORDS_PER_PAGE);
			if (resultPage.getPageNo() < 1)
				resultPage.setPageNo(1);
			else if (resultPage.getPageNo() > resultPage.getTotalPage())
				resultPage.setPageNo(resultPage.getTotalPage());
		}
		resultPage.getDetachedCriteria().setProjection(null);
		resultPage.getDetachedCriteria().setResultTransformer(
				CriteriaSpecification.ROOT_ENTITY);
		Map<String, Boolean> sorts = resultPage.getSorts();
		if (sorts.size() > 0) {
			for (Map.Entry<String, Boolean> entry : sorts.entrySet()) {
				if (entry.getValue())
					resultPage.getDetachedCriteria().addOrder(
							Order.desc(entry.getKey()));
				else
					resultPage.getDetachedCriteria().addOrder(
							Order.asc(entry.getKey()));
			}
		}
		int start, end;
		if (!resultPage.isReverse()) {
			start = (resultPage.getPageNo() - 1) * resultPage.getPageSize();
			end = resultPage.getPageNo() * resultPage.getPageSize();
		} else {
			start = (int) (resultPage.getTotalRecord() - resultPage.getPageNo()
					* resultPage.getPageSize());
			end = (int) (resultPage.getTotalRecord() - (resultPage.getPageNo() - 1)
					* resultPage.getPageSize());
		}
		if (!(resultPage.isCounting() && totalRecord == 0))
			resultPage.setResult(findBetweenListByCriteria(resultPage
					.getDetachedCriteria(), start, end));
		else
			resultPage.setResult(Collections.EMPTY_LIST);
		resultPage.setStart(start);
		return resultPage;
	}

	@Transactional(readOnly = true)
	public long countAll() {
		return countByCriteria(detachedCriteria());
	}

	@Transactional(readOnly = true)
	public List<T> findAll(Order... orders) {
		Criteria c = sessionFactory.getCurrentSession().createCriteria(
				getEntityClass());
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		for (Order order : orders)
			c.addOrder(order);
		return c.list();
	}

	@Transactional(readOnly = true)
	public T findByNaturalId(Object... objects) {
		if (objects.length == 1) {
			Criteria c = sessionFactory.getCurrentSession().createCriteria(
					getEntityClass());
			NaturalIdentifier ni = Restrictions.naturalId();
			Set<String> naturalIds = AnnotationUtils.getAnnotatedPropertyNames(
					getEntityClass(), NaturalId.class);
			if (naturalIds.size() != 1)
				throw new IllegalArgumentException(
						"@NaturalId must and only be one");
			ni.set(naturalIds.iterator().next(), objects[0]);
			c.add(ni);
			c.setMaxResults(1);
			return (T) c.uniqueResult();
		}
		if (objects.length == 0 || objects.length % 2 != 0)
			throw new IllegalArgumentException("parameter size must be even");
		Criteria c = sessionFactory.getCurrentSession().createCriteria(
				getEntityClass());
		NaturalIdentifier ni = Restrictions.naturalId();
		int doubles = objects.length / 2;
		for (int i = 0; i < doubles; i++)
			ni.set(String.valueOf(objects[2 * i]), objects[2 * i + 1]);
		c.add(ni);
		c.setMaxResults(1);
		return (T) c.uniqueResult();
	}

	@Transactional(readOnly = true)
	public T findByNaturalId(boolean caseInsensitive, Object... objects) {
		if (!caseInsensitive)
			return findByNaturalId(objects);
		String hql = "select entity from " + getEntityClass().getName()
				+ " entity where ";
		if (objects.length == 1) {
			Set<String> naturalIds = AnnotationUtils.getAnnotatedPropertyNames(
					getEntityClass(), NaturalId.class);
			if (naturalIds.size() != 1)
				throw new IllegalArgumentException(
						"@NaturalId must and only be one");
			hql += "lower(entity." + naturalIds.iterator().next()
					+ ")=lower(?)";
			Query query = sessionFactory.getCurrentSession().createQuery(hql);
			query.setParameter(0, objects[0]);
			query.setMaxResults(1);
			return (T) query.uniqueResult();
		}
		int doubles = objects.length / 2;
		if (doubles == 1) {
			hql += "lower(entity." + String.valueOf(objects[0]) + ")=lower(?)";
		} else {
			List<String> list = new ArrayList<String>(doubles);
			for (int i = 0; i < doubles; i++)
				list.add("lower(entity." + String.valueOf(objects[2 * i])
						+ ")=lower(?)");
			hql += StringUtils.join(list, " and ");
		}
		Query query = sessionFactory.getCurrentSession().createQuery(hql);
		for (int i = 0; i < doubles; i++)
			query.setParameter(i, objects[2 * i + 1]);
		query.setMaxResults(1);
		return (T) query.uniqueResult();
	}

	@Transactional(readOnly = true)
	public List<T> find(final String queryString, final Object... args) {
		Query query = sessionFactory.getCurrentSession().createQuery(
				queryString);
		for (int i = 0; i < args.length; i++)
			query.setParameter(i, args[i]);
		return query.list();
	}

	@Transactional(readOnly = true)
	public <TE extends BaseTreeableEntity<TE>> TE loadTree() {
		if (getEntityClass() == null
				|| !(BaseTreeableEntity.class
						.isAssignableFrom(getEntityClass())))
			throw new IllegalArgumentException(
					"entityClass mustn't be null,and must extends class 'BaseTreeableEntity'");
		try {
			TE root = (TE) getEntityClass().newInstance();
			root.setId(0L);
			root.setName("");
			assemble(root, (List<TE>) findAll());
			return root;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	private <TE extends BaseTreeableEntity<TE>> void assemble(TE te,
			List<TE> list) throws Exception {
		List<TE> children = new ArrayList<TE>();
		for (TE r : list) {
			boolean isChild = false;
			if (te.getId() == 0) {
				if (r.getFullId().indexOf('.') < 0)
					isChild = true;
			} else {
				if (r.getFullId().indexOf('.') > 0
						&& te.getFullId().equals(
								r.getFullId().substring(0,
										r.getFullId().lastIndexOf('.'))))
					isChild = true;
			}
			if (isChild) {
				TE rr = (TE) te.getClass().newInstance();
				BeanUtils.copyProperties(r, rr);
				children.add(rr);
				rr.setParent(te);
			}
		}
		Collections.sort(children);
		te.setChildren(children);
		for (TE r : children)
			assemble(r, list);
	}

	@Transactional
	public int executeUpdate(String queryString, Object... values) {
		Query queryObject = sessionFactory.getCurrentSession().createQuery(
				queryString);
		SessionFactoryUtils
				.applyTransactionTimeout(queryObject, sessionFactory);
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				queryObject.setParameter(i, values[i]);
			}
		}
		return queryObject.executeUpdate();
	}

	@Transactional
	public Object execute(HibernateCallback callback) {
		try {
			return callback.doInHibernate(sessionFactory.getCurrentSession());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	@Transactional(readOnly = true)
	public Object executeFind(HibernateCallback callback) {
		return execute(callback);
	}

}