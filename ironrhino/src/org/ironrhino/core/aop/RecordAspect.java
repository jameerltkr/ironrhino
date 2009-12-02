package org.ironrhino.core.aop;

import java.util.Date;

import javax.inject.Named;
import javax.inject.Singleton;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.common.model.Record;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.metadata.RecordAware;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.core.Ordered;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.userdetails.UserDetails;

/**
 * Use for record model's CRUD operations
 * 
 * @author zhouyanming
 * @see org.ironrhino.core.metadata.RecordAware
 */
@Aspect
@Singleton
@Named
public class RecordAspect extends HibernateDaoSupport implements Ordered {

	public RecordAspect() {
		order = 1;
	}

	private int order;

	@Around("execution(* org.ironrhino..service.*Manager.save*(*)) and args(entity) and @args(recordAware)")
	public Object save(ProceedingJoinPoint call, Persistable entity,
			RecordAware recordAware) throws Throwable {
		if (AopContext.isBypass(this.getClass()))
			return call.proceed();
		boolean isNew = entity.isNew();
		Object result = call.proceed();
		record(entity, isNew ? EntityOperationType.CREATE
				: EntityOperationType.UPDATE);
		return result;
	}

	@AfterReturning("execution(* org.ironrhino..service.*Manager.delete*(*)) and args(entity) and @args(recordAware)")
	public void delete(Persistable entity, RecordAware recordAware) {
		if (AopContext.isBypass(this.getClass()))
			return;
		record(entity, EntityOperationType.DELETE);
	}

	// record to database,may change to use logger system
	private void record(Persistable entity, EntityOperationType action) {
		final Record record = new Record();
		UserDetails ud = AuthzUtils.getUserDetails(UserDetails.class);
		if (ud != null) {
			record.setOperatorId(ud.getUsername());
			record.setOperatorClass(ud.getClass().getName());
		}
		try {
			record.setEntityId(String.valueOf(entity.getId()));
		} catch (Exception e) {
		}
		record.setEntityClass(entity.getClass().getName());
		record.setEntityToString(entity.toString());
		record.setAction(action.name());
		record.setRecordDate(new Date());
		// import javax.inject.Inject; import ant! no transaction,inserted
		// before actual save entity and
		// ignore transaction rollback
		getHibernateTemplate().save(record);
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
