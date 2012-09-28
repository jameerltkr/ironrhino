package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.compass.core.CompassHit;
import org.compass.core.support.search.CompassSearchResults;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.NaturalId;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Ordered;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.search.CompassCriteria;
import org.ironrhino.core.search.CompassSearchService;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.config.PackageProvider;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;

public class EntityAction extends BaseAction {

	private static final long serialVersionUID = -8442983706126047413L;

	protected static Log log = LogFactory.getLog(EntityAction.class);

	private transient BaseManager<Persistable> baseManager;

	private ResultPage resultPage;

	private Persistable entity;

	private boolean readonly;

	private boolean searchable;

	@Autowired(required = false)
	private transient CompassSearchService compassSearchService;

	public boolean isSearchable() {
		return searchable;
	}

	public boolean isReadonly() {
		return readonly;
	}

	public Persistable getEntity() {
		return entity;
	}

	public ResultPage getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage resultPage) {
		this.resultPage = resultPage;
	}

	public void setBaseManager(BaseManager baseManager) {
		this.baseManager = baseManager;
	}

	private boolean readonly() {
		AutoConfig ac = (AutoConfig) getEntityClass().getAnnotation(
				AutoConfig.class);
		return (ac != null) && ac.readonly();
	}

	private boolean searchable() {
		AutoConfig ac = (AutoConfig) getEntityClass().getAnnotation(
				AutoConfig.class);
		return (ac != null) && ac.searchable();
	}

	private void checkEntityManager() {
		String entityManagerName = getEntityName() + "Manager";
		try {
			Object bean = ApplicationContextUtils.getBean(entityManagerName);
			if (bean != null)
				baseManager = (BaseManager) bean;
			else
				baseManager.setEntityClass(getEntityClass());
		} catch (NoSuchBeanDefinitionException e) {

		}
	}

	@Override
	public String list() {
		if (StringUtils.isBlank(keyword) || compassSearchService == null) {
			checkEntityManager();
			DetachedCriteria dc = baseManager.detachedCriteria();
			if (resultPage == null)
				resultPage = new ResultPage();
			resultPage.setDetachedCriteria(dc);
			if (Ordered.class.isAssignableFrom(getEntityClass()))
				dc.addOrder(Order.asc("displayOrder"));
			resultPage = baseManager.findByResultPage(resultPage);
		} else {
			String query = keyword.trim();
			CompassCriteria cc = new CompassCriteria();
			cc.setQuery(query);
			cc.setAliases(new String[] { getEntityName() });
			if (resultPage == null)
				resultPage = new ResultPage();
			cc.setPageNo(resultPage.getPageNo());
			cc.setPageSize(resultPage.getPageSize());
			CompassSearchResults searchResults = compassSearchService
					.search(cc);
			resultPage.setTotalRecord(searchResults.getTotalHits());
			CompassHit[] hits = searchResults.getHits();
			if (hits != null) {
				List list = new ArrayList(hits.length);
				for (CompassHit ch : searchResults.getHits()) {
					list.add(ch.getData());
				}
				resultPage.setResult(list);
			} else {
				resultPage.setResult(Collections.EMPTY_LIST);
			}
		}
		readonly = readonly();
		searchable = searchable();
		return LIST;
	}

	@Override
	public String input() {
		if (readonly())
			return ACCESSDENIED;
		checkEntityManager();
		if (getUid() != null) {
			try {
				BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass()
						.newInstance());
				bw.setPropertyValue("id", getUid());
				entity = baseManager.get((Serializable) bw
						.getPropertyValue("id"));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		if (entity == null)
			try {
				// for fetch default value by construct
				entity = (Persistable) getEntityClass().newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		setEntity(entity);
		return INPUT;
	}

	@Override
	public String save() {
		if (readonly())
			return ACCESSDENIED;
		checkEntityManager();
		entity = constructEntity();
		BeanWrapperImpl bw = new BeanWrapperImpl(entity);
		Persistable persisted = null;
		Map<String, Annotation> naturalIds = getNaturalIds();
		boolean naturalIdImmutable = isNaturalIdsImmatuable();
		boolean caseInsensitive = naturalIds.size() > 0
				&& ((NaturalId) naturalIds.values().iterator().next())
						.caseInsensitive();
		if (entity.isNew()) {
			if (naturalIds.size() > 0) {
				Object[] args = new Object[naturalIds.size() * 2];
				Iterator<String> it = naturalIds.keySet().iterator();
				int i = 0;
				try {
					while (it.hasNext()) {
						String name = it.next();
						args[i] = name;
						i++;
						args[i] = bw.getPropertyValue(name);
						i++;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				persisted = baseManager.findByNaturalId(caseInsensitive, args);
				if (persisted != null) {
					it = naturalIds.keySet().iterator();
					while (it.hasNext()) {
						addFieldError(getEntityName() + "." + it.next(),
								getText("validation.already.exists"));
					}
					return INPUT;
				}
			}
		} else {
			if (!naturalIdImmutable && naturalIds.size() > 0) {
				Object[] args = new Object[naturalIds.size() * 2];
				Iterator<String> it = naturalIds.keySet().iterator();
				int i = 0;
				try {
					while (it.hasNext()) {
						String name = it.next();
						args[i] = name;
						i++;
						args[i] = bw.getPropertyValue(name);
						i++;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				persisted = baseManager.findByNaturalId(caseInsensitive, args);
				if (persisted != null
						&& !persisted.getId().equals(entity.getId())) {
					it = naturalIds.keySet().iterator();
					while (it.hasNext()) {
						addFieldError(getEntityName() + "." + it.next(),
								getText("validation.already.exists"));
					}
					return INPUT;
				}
				if (persisted != null
						&& !persisted.getId().equals(entity.getId())) {
					persisted = null;
				}
			}
			try {
				if (persisted == null)
					persisted = baseManager.get((Serializable) bw
							.getPropertyValue("id"));
				BeanWrapperImpl bwp = new BeanWrapperImpl(persisted);
				Set<String> names = getUiConfigs().keySet();
				if (naturalIdImmutable)
					names.removeAll(naturalIds.keySet());
				for (String name : names) 
						bwp.setPropertyValue(name, bw.getPropertyValue(name));
				entity = persisted;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		baseManager.save(entity);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	@Override
	public String view() {
		checkEntityManager();
		if (getUid() != null) {
			try {
				BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass()
						.newInstance());
				bw.setPropertyValue("id", getUid());
				entity = baseManager.get((Serializable) bw
						.getPropertyValue("id"));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		setEntity(entity);
		return VIEW;
	}

	@Override
	public String delete() {
		if (readonly())
			return ACCESSDENIED;
		checkEntityManager();
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length]
				: new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass()
					.newInstance());
			for (int i = 0; i < id.length; i++) {
				bw.setPropertyValue("id", arr[i]);
				id[i] = (Serializable) bw.getPropertyValue("id");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if (id.length > 0) {
			List list;
			if (id.length == 1) {
				list = new ArrayList(1);
				list.add(baseManager.get(id[0]));
			} else {
				DetachedCriteria dc = baseManager.detachedCriteria();
				dc.add(Restrictions.in("id", id));
				list = baseManager.findListByCriteria(dc);
			}

			if (list.size() > 0) {
				boolean deletable = true;
				for (Object obj : list)
					if (!baseManager.canDelete((Persistable) obj)) {
						deletable = false;
						addActionError(getText("delete.forbidden"));
						break;
					}
				if (deletable) {
					for (Object obj : list)
						baseManager.delete((Persistable) obj);
					addActionMessage(getText("delete.success"));
				}
			}

		}
		return SUCCESS;
	}

	@Override
	protected Authorize findAuthorize() {
		Class<?> c = getEntityClass();
		return c.getAnnotation(Authorize.class);
	}

	public boolean isNew() {
		return entity == null || entity.isNew();
	}

	private Map<String, Annotation> _naturalIds;

	// need call once before view
	public String getEntityName() {
		if (entityName == null)
			entityName = ActionContext.getContext().getActionInvocation()
					.getProxy().getActionName();
		return entityName;
	}

	private String entityName;

	public Map<String, Annotation> getNaturalIds() {
		if (_naturalIds != null)
			return _naturalIds;
		_naturalIds = AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(
				getEntityClass(), NaturalId.class);
		return _naturalIds;
	}

	public boolean isNaturalIdsImmatuable() {
		return getNaturalIds().size() > 0
				&& ((NaturalId) getNaturalIds().values().iterator().next())
						.immutable();
	}

	public Map<String, UiConfigImpl> getUiConfigs() {
		Class clazz = getEntityClass();
		String key = clazz.getName();
		if (cache.get(key) == null) {
			Set<String> hides = new HashSet<String>();
			hides.addAll(AnnotationUtils.getAnnotatedPropertyNames(clazz,
					NotInCopy.class));
			Map<String, UiConfigImpl> map = new HashMap<String, UiConfigImpl>();
			PropertyDescriptor[] pds = org.springframework.beans.BeanUtils
					.getPropertyDescriptors(clazz);
			for (PropertyDescriptor pd : pds) {
				UiConfig uiConfig = pd.getReadMethod().getAnnotation(
						UiConfig.class);
				if (uiConfig == null)
					try {
						Field f = clazz.getDeclaredField(pd.getName());
						if (f != null)
							uiConfig = f.getAnnotation(UiConfig.class);
					} catch (Exception e) {
					}
				if (uiConfig != null && uiConfig.hide())
					continue;
				if ("new".equals(pd.getName()) || "id".equals(pd.getName())
						|| "class".equals(pd.getName())
						|| pd.getReadMethod() == null
						|| hides.contains(pd.getName()))
					continue;
				Class returnType = pd.getReadMethod().getReturnType();
				if (returnType.isEnum()) {
					UiConfigImpl fes = new UiConfigImpl();
					try {
						Method method = pd.getReadMethod().getReturnType()
								.getMethod("values", new Class[0]);
						fes.setEnumValues((Enum[]) method.invoke(null));
						fes.setEnumClass(returnType.getName());
						fes.setType("select");
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					map.put(pd.getName(), fes);
					continue;
				}
				UiConfigImpl fec = new UiConfigImpl(uiConfig);
				if (returnType == Integer.TYPE || returnType == Integer.class
						|| returnType == Short.TYPE
						|| returnType == Short.class || returnType == Long.TYPE
						|| returnType == Long.class) {
					fec.addCssClass("integer");
				} else if (returnType == Double.TYPE
						|| returnType == Double.class
						|| returnType == Float.TYPE
						|| returnType == Float.class
						|| returnType == BigDecimal.class) {
					fec.addCssClass("double");
				} else if (Date.class.isAssignableFrom(returnType)) {
					fec.addCssClass("date");
				} else if (String.class == returnType
						&& pd.getName().toLowerCase().contains("email")) {
					fec.addCssClass("email");
				} else if (returnType == Boolean.TYPE
						|| returnType == Boolean.class)
					fec.setType("checkbox");
				if (getNaturalIds().containsKey(pd.getName()))
					fec.setRequired(true);
				map.put(pd.getName(), fec);
			}
			List<Map.Entry<String, UiConfigImpl>> list = new ArrayList<Map.Entry<String, UiConfigImpl>>();
			list.addAll(map.entrySet());
			Collections.sort(list,
					new Comparator<Map.Entry<String, UiConfigImpl>>() {
						public int compare(Entry<String, UiConfigImpl> o1,
								Entry<String, UiConfigImpl> o2) {
							int i = Integer.valueOf(
									o1.getValue().getDisplayOrder()).compareTo(
									o2.getValue().getDisplayOrder());
							if (i == 0)
								return o1.getKey().compareTo(o2.getKey());
							else
								return i;
						}
					});
			map = new LinkedHashMap<String, UiConfigImpl>();
			for (Map.Entry<String, UiConfigImpl> entry : list)
				map.put(entry.getKey(), entry.getValue());
			cache.put(key, map);
		}
		return cache.get(key);
	}

	private static Map<String, Map<String, UiConfigImpl>> cache = new ConcurrentHashMap<String, Map<String, UiConfigImpl>>();

	public static class UiConfigImpl {

		private String type = UiConfig.DEFAULT_TYPE;
		private int size;
		private String enumClass;
		private Enum[] enumValues;
		private String cssClass = "";
		private boolean readonly;
		private int displayOrder;
		private String displayName;
		private String template;

		public UiConfigImpl() {
		}

		public UiConfigImpl(UiConfig config) {
			if (config == null)
				return;
			this.type = config.type();
			this.size = config.size();
			this.readonly = config.readonly();
			if (config.required())
				this.setRequired(true);
			this.displayOrder = config.displayOrder();
			if (StringUtils.isNotBlank(config.displayName()))
				this.displayName = config.displayName();
			this.template = config.template();
		}

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}

		public int getDisplayOrder() {
			return displayOrder;
		}

		public void setDisplayOrder(int displayOrder) {
			this.displayOrder = displayOrder;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public int getSize() {
			return size;
		}

		public String getEnumClass() {
			return enumClass;
		}

		public void setEnumClass(String enumClass) {
			this.enumClass = enumClass;
		}

		public void setSize(int size) {
			this.size = size;
		}

		public Enum[] getEnumValues() {
			return enumValues;
		}

		public void setEnumValues(Enum[] enumValues) {
			this.enumValues = enumValues;
		}

		public String getCssClass() {
			return cssClass;
		}

		public void addCssClass(String cssClass) {
			this.cssClass += " " + cssClass;
		}

		public void setRequired(boolean required) {
			if (required)
				this.cssClass += " required";
		}

		public boolean isReadonly() {
			return readonly;
		}

		public void setReadonly(boolean readonly) {
			this.readonly = readonly;
		}

		public String getTemplate() {
			return template;
		}

		public void setTemplate(String template) {
			this.template = template;
		}

	}

	// need call once before view
	private Class getEntityClass() {
		if (entityClass == null) {
			ActionProxy proxy = ActionContext.getContext()
					.getActionInvocation().getProxy();
			String actionName = getEntityName();
			String namespace = proxy.getNamespace();
			entityClass = ((AutoConfigPackageProvider) packageProvider)
					.getEntityClass(namespace, actionName);
		}
		return entityClass;
	}

	private Class entityClass;

	private void setEntity(Persistable entity) {
		ValueStack vs = ActionContext.getContext().getValueStack();
		vs.set(getEntityName(), entity);
	}

	private Persistable constructEntity() {
		Persistable entity = null;
		try {
			entity = (Persistable) getEntityClass().newInstance();
			Map<String, String[]> map = ServletActionContext.getRequest()
					.getParameterMap();
			ValueStack temp = valueStackFactory.createValueStack();
			temp.set(getEntityName(), entity);
			for (Map.Entry<String, String[]> entry : map.entrySet())
				temp.setValue(entry.getKey(), entry.getValue());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return entity;
	}

	@Inject
	private transient ValueStackFactory valueStackFactory;

	@Inject("ironrhino-autoconfig")
	private transient PackageProvider packageProvider;

}