package org.ironrhino.core.struts;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Owner;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Ordered;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.search.SearchService.Mapper;
import org.ironrhino.core.search.elasticsearch.ElasticSearchCriteria;
import org.ironrhino.core.search.elasticsearch.ElasticSearchService;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.struts.AnnotationShadows.ReadonlyImpl;
import org.ironrhino.core.struts.AnnotationShadows.RichtableImpl;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.core.userdetails.UserDetails;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;
import com.opensymphony.xwork2.util.ValueStackFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionContextState;

import freemarker.template.Template;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class EntityAction<EN extends Persistable<?>> extends BaseAction {

	private static final long serialVersionUID = -8442983706126047413L;

	protected static Logger log = LoggerFactory.getLogger(EntityAction.class);

	private ReadonlyImpl _readonly;

	private RichtableImpl _richtableConfig;

	private Map<String, UiConfigImpl> _uiConfigs;

	private Persistable _entity;

	private String _entityName;

	private Map<String, NaturalId> _naturalIds;

	protected ResultPage resultPage;

	@Autowired(required = false)
	private transient ElasticSearchService<Persistable<?>> elasticSearchService;

	@Autowired(required = false)
	private transient ConversionService conversionService;

	public boolean isSearchable() {
		if (getEntityClass().getAnnotation(Searchable.class) != null)
			return true;
		RichtableImpl rc = getRichtableConfig();
		boolean searchable = rc.isSearchable();
		if (searchable)
			return true;
		else
			for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs()
					.entrySet())
				if (entry.getValue().isSearchable())
					return true;
		return false;
	}

	public boolean isEnableable() {
		return Enableable.class.isAssignableFrom(getEntityClass());
	}

	public Persistable getEntity() {
		return _entity;
	}

	public ResultPage getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage resultPage) {
		this.resultPage = resultPage;
	}

	public RichtableImpl getRichtableConfig() {
		if (_richtableConfig == null) {
			Richtable rc = getClass().getAnnotation(Richtable.class);
			if (rc == null)
				rc = getEntityClass().getAnnotation(Richtable.class);
			_richtableConfig = new RichtableImpl(rc);
		}
		return _richtableConfig;
	}

	public ReadonlyImpl getReadonly() {
		if (_readonly == null) {
			Richtable rconfig = getClass().getAnnotation(Richtable.class);
			if (rconfig == null)
				rconfig = getEntityClass().getAnnotation(Richtable.class);
			Readonly rc = null;
			if (rconfig != null)
				rc = rconfig.readonly();
			Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();
			if (rc == null && ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!owner.isolate()
						&& owner.readonlyForOther()
						&& !(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
								.authorize(null, owner.supervisorRole(), null))) {
					_readonly = new ReadonlyImpl();
					_readonly.setValue(false);
					StringBuilder sb = new StringBuilder("!entity.");
					sb.append(ownerProperty.getKey().propertyName())
							.append("?? || entity.")
							.append(ownerProperty.getKey().propertyName())
							.append("!=authentication('principal')");
					String expression = sb.toString();
					_readonly.setExpression(expression);
					_readonly.setDeletable(false);
				}
			}
			if (_readonly == null)
				_readonly = new ReadonlyImpl(rc);
		}
		return _readonly;
	}

	public String getEntityName() {
		if (_entityName == null)
			_entityName = ActionContext.getContext().getActionInvocation()
					.getProxy().getActionName();
		return _entityName;
	}

	public Map<String, NaturalId> getNaturalIds() {
		if (_naturalIds == null)
			_naturalIds = AnnotationUtils
					.getAnnotatedPropertyNameAndAnnotations(getEntityClass(),
							NaturalId.class);
		return _naturalIds;
	}

	public boolean isNaturalIdMutable() {
		return getNaturalIds().size() > 0
				&& ((NaturalId) getNaturalIds().values().iterator().next())
						.mutable();
	}

	public Map<String, UiConfigImpl> getUiConfigs() {
		if (_uiConfigs == null)
			_uiConfigs = EntityClassHelper.getUiConfigs(getEntityClass());
		return _uiConfigs;
	}

	protected <T extends Persistable<?>> BaseManager<T> getEntityManager(
			Class<T> entityClass) {
		return ApplicationContextUtils.getEntityManager(entityClass);
	}

	private void tryFindEntity() {
		BaseManager entityManager = getEntityManager(getEntityClass());
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass()
					.newInstance());
			bw.setConversionService(conversionService);
			Set<String> naturalIds = getNaturalIds().keySet();
			if (StringUtils.isNotBlank(getUid())) {
				bw.setPropertyValue("id", getUid());
				Serializable id = (Serializable) bw.getPropertyValue("id");
				_entity = entityManager.get(id);
				if (_entity == null && naturalIds.size() == 1) {
					String naturalIdName = naturalIds.iterator().next();
					bw.setPropertyValue(naturalIdName, getUid());
					id = (Serializable) bw.getPropertyValue(naturalIdName);
					_entity = entityManager.findByNaturalId(id);
				}
			}
			if (_entity == null && naturalIds.size() > 0) {
				Serializable[] paramters = new Serializable[naturalIds.size() * 2];
				int i = 0;
				boolean satisfied = true;
				for (String naturalId : naturalIds) {
					paramters[i] = naturalId;
					i++;
					bw.setPropertyValue(naturalId, ServletActionContext
							.getRequest().getParameter(naturalId));
					Serializable value = (Serializable) bw
							.getPropertyValue(naturalId);
					if (value != null)
						paramters[i] = value;
					else {
						satisfied = false;
						break;
					}
					i++;
				}
				if (satisfied)
					_entity = entityManager.findByNaturalId(paramters);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public String execute() {
		BeanWrapperImpl bw;
		try {
			bw = new BeanWrapperImpl(getEntityClass().newInstance());
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
		bw.setConversionService(conversionService);
		Richtable richtableConfig = getClass().getAnnotation(Richtable.class);
		if (richtableConfig == null)
			richtableConfig = getEntityClass().getAnnotation(Richtable.class);
		final BaseManager entityManager = getEntityManager(getEntityClass());
		Map<String, String> aliases = new HashMap<String, String>();
		boolean searchable = isSearchable();
		if (searchable
				&& StringUtils.isNumeric(keyword)
				|| StringUtils.isAlphanumeric(keyword)
				&& (keyword.length() == 32 || keyword.length() >= 22
						&& keyword.length() <= 24)) // keyword is id
			try {
				bw.setPropertyValue("id", keyword);
				Serializable idvalue = (Serializable) bw.getPropertyValue("id");
				if (idvalue != null) {
					Persistable p = getEntityManager(getEntityClass()).get(
							idvalue);
					if (p != null) {
						resultPage = new ResultPage();
						resultPage.setPageNo(1);
						resultPage.setTotalResults(1);
						resultPage.setResult(Collections.singletonList(p));
						return LIST;
					}
				}
			} catch (Exception e) {

			}

		Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null
				&& ownerProperty.getKey().isolate()
				|| (!searchable || StringUtils.isBlank(keyword) || (searchable && elasticSearchService == null))) {
			DetachedCriteria dc = entityManager.detachedCriteria();
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
						.authorize(null, owner.supervisorRole(), null))
						&& owner.isolate()) {
					UserDetails ud = AuthzUtils.getUserDetails(ownerProperty
							.getValue());
					if (ud == null)
						return ACCESSDENIED;
					dc.add(Restrictions.eq(owner.propertyName(), ud));
				}
			}
			CriteriaState criteriaState = CriterionUtils.filter(dc,
					getEntityClass(), getUiConfigs());
			prepare(dc, criteriaState);
			if (searchable && StringUtils.isNotBlank(keyword)) {
				Set<String> propertyNamesInLike = new HashSet<String>();
				for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs()
						.entrySet()) {
					if (entry.getValue().isSearchable()
							&& String.class.equals(entry.getValue()
									.getPropertyType())
							&& !entry.getValue().isExcludedFromLike())
						propertyNamesInLike.add(entry.getKey());
				}
				if (propertyNamesInLike.size() > 0)
					dc.add(CriterionUtils.like(keyword, MatchMode.ANYWHERE,
							propertyNamesInLike.toArray(new String[0])));
			}
			if (resultPage == null)
				resultPage = new ResultPage();
			resultPage.setCriteria(dc);
			if (criteriaState == null || criteriaState.getOrderings().isEmpty()) {
				if (richtableConfig != null
						&& StringUtils.isNotBlank(richtableConfig.order())) {
					String[] ar = richtableConfig.order().split(",");
					for (String s : ar) {
						String[] arr = s.split("\\s+", 2);
						String propertyName = arr[0];
						if (propertyName.indexOf(".") > 0) {
							String p1 = propertyName.substring(0,
									propertyName.indexOf("."));
							String p2 = propertyName.substring(propertyName
									.indexOf(".") + 1);
							Class type = bw.getPropertyType(p1);
							if (Persistable.class.isAssignableFrom(type)) {
								String alias = aliases.get(p1);
								if (alias == null) {
									alias = CodecUtils.randomString(4);
									dc.createAlias(p1, alias);
									aliases.put(p1, alias);
								}
								propertyName = alias + "." + p2;
							}
						}
						if (arr.length == 2 && arr[1].equalsIgnoreCase("asc"))
							dc.addOrder(Order.asc(propertyName));
						else if (arr.length == 2
								&& arr[1].equalsIgnoreCase("desc"))
							dc.addOrder(Order.desc(propertyName));
						else
							dc.addOrder(Order.asc(propertyName));
					}
				} else if (Ordered.class.isAssignableFrom(getEntityClass()))
					dc.addOrder(Order.asc("displayOrder"));
			}
			resultPage = entityManager.findByResultPage(resultPage);
		} else {
			Set<String> searchableProperties = new HashSet<String>();
			for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs()
					.entrySet()) {
				if (entry.getValue().isSearchable())
					searchableProperties.add(entry.getKey());
			}
			String query = keyword.trim();
			ElasticSearchCriteria criteria = new ElasticSearchCriteria();
			criteria.setQuery(query);
			criteria.setTypes(new String[] { getEntityName() });
			if (richtableConfig != null
					&& StringUtils.isNotBlank(richtableConfig.order())) {
				String[] ar = richtableConfig.order().split(",");
				for (String s : ar) {
					String[] arr = s.split("\\s+", 2);
					String propertyName = arr[0];
					if (searchableProperties.contains(propertyName)) {
						if (arr.length == 2 && arr[1].equalsIgnoreCase("asc"))
							criteria.addSort(propertyName, false);
						else if (arr.length == 2
								&& arr[1].equalsIgnoreCase("desc"))
							criteria.addSort(propertyName, true);
						else
							criteria.addSort(propertyName, false);
					}
				}
			} else if (Ordered.class.isAssignableFrom(getEntityClass())
					&& searchableProperties.contains("displayOrder"))
				criteria.addSort("displayOrder", false);
			if (resultPage == null)
				resultPage = new ResultPage();
			resultPage.setCriteria(criteria);
			resultPage = elasticSearchService.search(resultPage,
					new Mapper<Persistable<?>>() {
						@Override
						public Persistable map(Persistable source) {
							return entityManager.get(source.getId());
						}
					});
		}
		return LIST;
	}

	protected void prepare(DetachedCriteria dc, CriteriaState criteriaState) {

	}

	@Override
	public String input() {
		if (getReadonly().isValue()) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		return doInput();
	}

	protected String doInput() {
		tryFindEntity();
		if (_entity != null && !_entity.isNew()) {
			Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
						.authorize(null, owner.supervisorRole(), null))
						&& (owner.isolate() || owner.readonlyForOther())) {
					UserDetails ud = AuthzUtils.getUserDetails(ownerProperty
							.getValue());
					BeanWrapperImpl bwi = new BeanWrapperImpl(_entity);
					bwi.setConversionService(conversionService);
					Object value = bwi.getPropertyValue(owner.propertyName());
					if (ud == null || value == null || !ud.equals(value)) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			if (checkEntityReadonly(getReadonly().getExpression(), _entity)) {
				addActionError(getText("access.denied"));
				return ACCESSDENIED;
			}
		}
		if (_entity == null)
			try {
				_entity = getEntityClass().newInstance();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		BeanWrapperImpl bw = new BeanWrapperImpl(_entity);
		bw.setConversionService(conversionService);
		if (_entity != null && _entity.isNew()) {
			Set<String> naturalIds = getNaturalIds().keySet();
			if (getUid() != null && naturalIds.size() == 1) {
				bw.setPropertyValue(naturalIds.iterator().next(), getUid());
			}
		}
		Set<String> editablePropertyNames = getUiConfigs().keySet();
		for (String parameterName : ServletActionContext.getRequest()
				.getParameterMap().keySet()) {
			String propertyName = parameterName;
			if (propertyName.startsWith(getEntityName() + "."))
				propertyName = propertyName
						.substring(propertyName.indexOf('.') + 1);
			if (propertyName.indexOf('.') > 0) {
				String subPropertyName = propertyName.substring(propertyName
						.indexOf('.') + 1);
				propertyName = propertyName.substring(0,
						propertyName.indexOf('.'));
				if (editablePropertyNames.contains(propertyName)) {
					Class type = bw.getPropertyType(propertyName);
					if (Persistable.class.isAssignableFrom(type)) {
						String parameterValue = ServletActionContext
								.getRequest().getParameter(parameterName);
						BaseManager em = getEntityManager(type);
						Persistable value = null;
						BeanWrapperImpl bw2 = new BeanWrapperImpl(type);
						bw2.setConversionService(conversionService);
						if (subPropertyName.equals("id")) {
							bw2.setPropertyValue("id", parameterValue);
							value = em.get((Serializable) bw2
									.getPropertyValue("id"));
						} else {
							bw2.setPropertyValue(subPropertyName,
									parameterValue);
							value = em.findOne(subPropertyName,
									(Serializable) bw2
											.getPropertyValue(subPropertyName));
						}
						bw.setPropertyValue(propertyName, value);
					}
				}
			} else if (editablePropertyNames.contains(propertyName)) {
				String parameterValue = ServletActionContext.getRequest()
						.getParameter(parameterName);
				Class type = bw.getPropertyType(propertyName);
				Object value = null;
				if (Persistable.class.isAssignableFrom(type)) {
					BaseManager em = getEntityManager(type);
					value = em.get(parameterValue);
					if (value == null) {
						try {
							value = em.findOne(parameterValue);
						} catch (Exception e) {

						}
					}
					bw.setPropertyValue(propertyName, value);
				} else {
					bw.setPropertyValue(propertyName, parameterValue);
				}
			}

		}
		putEntityToValueStack(_entity);
		return INPUT;
	}

	@Override
	public String save() {
		if (getReadonly().isValue()) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		return doSave();
	}

	protected String doSave() {
		if (!makeEntityValid())
			return INPUT;
		BeanWrapperImpl bwp = new BeanWrapperImpl(_entity);
		bwp.setConversionService(conversionService);
		Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();
		if (!_entity.isNew()) {
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
						.authorize(null, owner.supervisorRole(), null))
						&& (ownerProperty.getKey().isolate() || ownerProperty
								.getKey().readonlyForOther())) {
					UserDetails ud = AuthzUtils.getUserDetails(ownerProperty
							.getValue());
					Object value = bwp.getPropertyValue(owner.propertyName());
					if (ud == null || value == null || !ud.equals(value)) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			if (checkEntityReadonly(getReadonly().getExpression(), _entity)) {
				addActionError(getText("access.denied"));
				return ACCESSDENIED;
			}
		} else {
			if (ownerProperty != null) {
				UserDetails ud = AuthzUtils.getUserDetails(ownerProperty
						.getValue());
				if (ud == null) {
					addActionError(getText("access.denied"));
					return ACCESSDENIED;
				}
				bwp.setPropertyValue(ownerProperty.getKey().propertyName(), ud);
			}
		}

		for (Map.Entry<String, UiConfigImpl> entry : getUiConfigs().entrySet()) {
			String name = entry.getKey();
			UiConfigImpl uiconfig = entry.getValue();
			Object value = bwp.getPropertyValue(name);
			if (uiconfig.isRequired()
					&& (value == null || value instanceof String
							&& StringUtils.isBlank(value.toString()))) {
				addFieldError(getEntityName() + "." + name,
						getText("validation.required"));
				return INPUT;
			}
		}
		BaseManager<Persistable<?>> entityManager = getEntityManager(getEntityClass());
		entityManager.save(_entity);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	public String checkavailable() {
		return makeEntityValid() ? NONE : INPUT;
	}

	private boolean makeEntityValid() {
		boolean fromList = "cell".equalsIgnoreCase(ServletActionContext
				.getRequest().getHeader("X-Edit"));
		Map<String, UiConfigImpl> uiConfigs = getUiConfigs();
		BaseManager<Persistable<?>> entityManager = getEntityManager(getEntityClass());
		_entity = constructEntity();
		BeanWrapperImpl bw = new BeanWrapperImpl(_entity);
		bw.setConversionService(conversionService);
		for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet()) {
			String regex = entry.getValue().getRegex();
			if (StringUtils.isNotBlank(regex)) {
				Object value = bw.getPropertyValue(entry.getKey());
				if (value instanceof String) {
					String str = (String) value;
					if (StringUtils.isNotBlank(str) && !str.matches(regex)) {
						addFieldError(getEntityName() + "." + entry.getKey(),
								getText("validation.invalid"));
						return false;
					}
				}
			}
		}
		Persistable persisted = null;
		Map<String, NaturalId> naturalIds = getNaturalIds();
		boolean naturalIdMutable = isNaturalIdMutable();
		boolean caseInsensitive = AnnotationUtils
				.getAnnotatedPropertyNameAndAnnotations(getEntityClass(),
						CaseInsensitive.class).size() > 0;
		if (_entity.isNew()) {
			if (naturalIds.size() > 0) {
				Serializable[] args = new Serializable[naturalIds.size() * 2];
				Iterator<String> it = naturalIds.keySet().iterator();
				int i = 0;
				try {
					while (it.hasNext()) {
						String name = it.next();
						args[i] = name;
						i++;
						args[i] = (Serializable) bw.getPropertyValue(name);
						i++;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				persisted = entityManager.findOne(caseInsensitive, args);
				if (persisted != null) {
					it = naturalIds.keySet().iterator();
					while (it.hasNext()) {
						addFieldError(getEntityName() + "." + it.next(),
								getText("validation.already.exists"));
					}
					return false;
				}
				for (Map.Entry<String, UiConfigImpl> entry : uiConfigs
						.entrySet())
					if (entry.getValue().isUnique()
							&& StringUtils.isNotBlank(ServletActionContext
									.getRequest().getParameter(
											getEntityName() + '.'
													+ entry.getKey()))) {
						persisted = entityManager.findOne(entry.getKey(),
								(Serializable) bw.getPropertyValue(entry
										.getKey()));
						if (persisted != null) {
							addFieldError(
									getEntityName() + "." + entry.getKey(),
									getText("validation.already.exists"));
							return false;
						}
					}
			}
			try {
				Persistable temp = _entity;
				_entity = getEntityClass().newInstance();
				bw = new BeanWrapperImpl(temp);
				bw.setConversionService(conversionService);
				BeanWrapperImpl bwp = new BeanWrapperImpl(_entity);
				bwp.setConversionService(conversionService);
				Set<String> editedPropertyNames = new HashSet<String>();
				String propertyName = null;
				for (String parameterName : ServletActionContext.getRequest()
						.getParameterMap().keySet()) {
					if (parameterName.startsWith(getEntityName() + '.')
							|| parameterName.startsWith("__checkbox_"
									+ getEntityName() + '.')
							|| parameterName.startsWith("__datagrid_"
									+ getEntityName() + '.')) {
						propertyName = parameterName.substring(parameterName
								.indexOf('.') + 1);
						if (propertyName.indexOf('.') > 0)
							propertyName = propertyName.substring(0,
									propertyName.indexOf('.'));
						if (propertyName.indexOf('[') > 0)
							propertyName = propertyName.substring(0,
									propertyName.indexOf('['));
					}
					UiConfigImpl uiConfig = uiConfigs.get(propertyName);
					if (uiConfig == null
							|| uiConfig.getReadonly().isValue()
							|| fromList
							&& uiConfig.getHiddenInList().isValue()
							|| !fromList
							&& uiConfig.getHiddenInInput().isValue()
							|| Persistable.class.isAssignableFrom(bwp
									.getPropertyDescriptor(propertyName)
									.getPropertyType()))
						continue;
					if (StringUtils.isNotBlank(uiConfig.getReadonly()
							.getExpression())
							&& evalBoolean(uiConfig.getReadonly()
									.getExpression(), _entity,
									bwp.getPropertyValue(propertyName)))
						continue;
					if (fromList) {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInList()
								.getExpression())
								&& evalBoolean(uiConfig.getHiddenInList()
										.getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					} else {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInInput()
								.getExpression())
								&& evalBoolean(uiConfig.getHiddenInInput()
										.getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					}
					editedPropertyNames.add(propertyName);
				}
				for (String name : editedPropertyNames)
					bwp.setPropertyValue(name, bw.getPropertyValue(name));
				bw = bwp;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			if (naturalIdMutable && naturalIds.size() > 0) {
				Serializable[] args = new Serializable[naturalIds.size() * 2];
				Iterator<String> it = naturalIds.keySet().iterator();
				int i = 0;
				try {
					while (it.hasNext()) {
						String name = it.next();
						args[i] = name;
						i++;
						args[i] = (Serializable) bw.getPropertyValue(name);
						i++;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				persisted = entityManager.findOne(caseInsensitive, args);
				entityManager.evict(persisted);
				if (persisted != null
						&& !persisted.getId().equals(_entity.getId())) {
					it = naturalIds.keySet().iterator();
					while (it.hasNext()) {
						addFieldError(getEntityName() + "." + it.next(),
								getText("validation.already.exists"));
					}
					return false;
				}

				for (Map.Entry<String, UiConfigImpl> entry : uiConfigs
						.entrySet())
					if (entry.getValue().isUnique()
							&& StringUtils.isNotBlank(ServletActionContext
									.getRequest().getParameter(
											getEntityName() + '.'
													+ entry.getKey()))) {
						persisted = entityManager.findOne(entry.getKey(),
								(Serializable) bw.getPropertyValue(entry
										.getKey()));
						entityManager.evict(persisted);
						if (persisted != null
								&& !persisted.getId().equals(_entity.getId())) {
							addFieldError(
									getEntityName() + "." + entry.getKey(),
									getText("validation.already.exists"));
							return false;
						}
					}

				if (persisted != null
						&& !persisted.getId().equals(_entity.getId())) {
					persisted = null;
				}
			}
			try {
				if (persisted == null)
					persisted = entityManager.get((Serializable) bw
							.getPropertyValue("id"));
				BeanWrapperImpl bwp = new BeanWrapperImpl(persisted);
				bwp.setConversionService(conversionService);
				Set<String> editedPropertyNames = new HashSet<String>();
				String propertyName = null;
				for (String parameterName : ServletActionContext.getRequest()
						.getParameterMap().keySet()) {
					if (parameterName.startsWith(getEntityName() + '.')
							|| parameterName.startsWith("__checkbox_"
									+ getEntityName() + '.')
							|| parameterName.startsWith("__datagrid_"
									+ getEntityName() + '.')) {
						propertyName = parameterName.substring(parameterName
								.indexOf('.') + 1);
						if (propertyName.indexOf('.') > 0)
							propertyName = propertyName.substring(0,
									propertyName.indexOf('.'));
						if (propertyName.indexOf('[') > 0)
							propertyName = propertyName.substring(0,
									propertyName.indexOf('['));
					}
					UiConfigImpl uiConfig = uiConfigs.get(propertyName);
					if (uiConfig == null
							|| uiConfig.getReadonly().isValue()
							|| fromList
							&& uiConfig.getHiddenInList().isValue()
							|| !fromList
							&& uiConfig.getHiddenInInput().isValue()
							|| !naturalIdMutable
							&& naturalIds.keySet().contains(propertyName)
							|| Persistable.class.isAssignableFrom(bwp
									.getPropertyDescriptor(propertyName)
									.getPropertyType()))
						continue;
					if (StringUtils.isNotBlank(uiConfig.getReadonly()
							.getExpression())
							&& evalBoolean(uiConfig.getReadonly()
									.getExpression(), persisted,
									bwp.getPropertyValue(propertyName)))
						continue;
					if (fromList) {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInList()
								.getExpression())
								&& evalBoolean(uiConfig.getHiddenInList()
										.getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					} else {
						if (StringUtils.isNotBlank(uiConfig.getHiddenInInput()
								.getExpression())
								&& evalBoolean(uiConfig.getHiddenInInput()
										.getExpression(), _entity,
										bwp.getPropertyValue(propertyName)))
							continue;
					}
					editedPropertyNames.add(propertyName);
				}
				for (String name : editedPropertyNames)
					bwp.setPropertyValue(name, bw.getPropertyValue(name));
				bw = bwp;
				_entity = persisted;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		try {
			for (String propertyName : getUiConfigs().keySet()) {
				UiConfigImpl uiConfig = getUiConfigs().get(propertyName);
				Class type = bw.getPropertyDescriptor(propertyName)
						.getPropertyType();
				if (uiConfig.getReadonly().isValue() || !naturalIdMutable
						&& naturalIds.keySet().contains(propertyName)
						&& !_entity.isNew()
						|| !Persistable.class.isAssignableFrom(type))
					continue;
				if (!_entity.isNew()
						&& StringUtils.isNotBlank(uiConfig.getReadonly()
								.getExpression())
						&& evalBoolean(uiConfig.getReadonly().getExpression(),
								_entity, bw.getPropertyValue(propertyName)))
					continue;
				String parameterValue = ServletActionContext.getRequest()
						.getParameter(getEntityName() + "." + propertyName);
				if (parameterValue == null)
					parameterValue = ServletActionContext.getRequest()
							.getParameter(
									getEntityName() + "." + propertyName
											+ ".id");
				if (parameterValue == null)
					parameterValue = ServletActionContext.getRequest()
							.getParameter(propertyName + "Id");
				if (parameterValue == null) {
					continue;
				} else if (StringUtils.isBlank(parameterValue)) {
					bw.setPropertyValue(propertyName, null);
				} else {
					String listKey = uiConfig.getListKey();
					BeanWrapperImpl temp = new BeanWrapperImpl(
							type.newInstance());
					temp.setConversionService(conversionService);
					temp.setPropertyValue(listKey, parameterValue);
					BaseManager em = getEntityManager(type);
					Object obj;
					if (listKey.equals(UiConfig.DEFAULT_LIST_KEY))
						obj = em.get((Serializable) temp
								.getPropertyValue(listKey));
					else
						obj = em.findOne(listKey,
								(Serializable) temp.getPropertyValue(listKey));
					bw.setPropertyValue(propertyName, obj);
					em = getEntityManager(getEntityClass());
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return true;
	}

	private boolean checkEntityReadonly(String expression, Persistable<?> entity) {
		if (StringUtils.isNotBlank(expression)) {
			try {
				Template template = new Template(null, "${(" + expression
						+ ")?string!}", freemarkerManager.getConfig());
				StringWriter sw = new StringWriter();
				Map<String, Object> rootMap = new HashMap<String, Object>(2, 1);
				rootMap.put("entity", entity);
				template.process(rootMap, sw);
				return sw.toString().equals("true");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private boolean evalBoolean(String expression, Persistable<?> entity,
			Object value) {
		if (StringUtils.isNotBlank(expression)) {
			try {
				Template template = new Template(null, "${(" + expression
						+ ")?string!}", freemarkerManager.getConfig());
				StringWriter sw = new StringWriter();
				Map<String, Object> rootMap = new HashMap<String, Object>(4, 1);
				rootMap.put("entity", entity);
				rootMap.put("value", value);
				template.process(rootMap, sw);
				return sw.toString().equals("true");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private Tuple<Owner, Class<? extends UserDetails>> getOwnerProperty() {
		Owner owner = getEntityClass().getAnnotation(Owner.class);
		if (owner == null)
			return null;
		String propertyName = owner.propertyName();
		if (StringUtils.isBlank(propertyName))
			return null;
		BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass());
		bw.setConversionService(conversionService);
		Class type = bw.getPropertyType(propertyName);
		if (type == null)
			throw new IllegalArgumentException("No Such property "
					+ propertyName + " of bean " + getEntityClass());
		if (!UserDetails.class.isAssignableFrom(type))
			throw new IllegalArgumentException("property " + propertyName
					+ " of bean " + getEntityClass() + " is not instanceof "
					+ UserDetails.class);
		return new Tuple<Owner, Class<? extends UserDetails>>(owner, type);
	}

	@Override
	public String view() {
		tryFindEntity();
		if (_entity == null)
			return NOTFOUND;
		Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();
		if (ownerProperty != null) {
			Owner owner = ownerProperty.getKey();
			if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
					.authorize(null, owner.supervisorRole(), null))
					&& owner.isolate()) {
				UserDetails ud = AuthzUtils.getUserDetails(ownerProperty
						.getValue());
				BeanWrapperImpl bwi = new BeanWrapperImpl(_entity);
				bwi.setConversionService(conversionService);
				Object value = bwi.getPropertyValue(owner.propertyName());
				if (ud == null || value == null || !ud.equals(value)) {
					addActionError(getText("access.denied"));
					return ACCESSDENIED;
				}
			}
		}
		putEntityToValueStack(_entity);
		return VIEW;
	}

	@Override
	public String delete() {
		if (getReadonly().isValue() && !getReadonly().isDeletable()) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		return doDelete();
	}

	protected String doDelete() {
		BaseManager<Persistable<?>> entityManager = getEntityManager(getEntityClass());
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length]
				: new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass()
					.newInstance());
			bw.setConversionService(conversionService);
			for (int i = 0; i < id.length; i++) {
				bw.setPropertyValue("id", arr[i]);
				id[i] = (Serializable) bw.getPropertyValue("id");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if (id.length > 0) {
			Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();

			UserDetails ud = null;
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
						.authorize(null, owner.supervisorRole(), null))) {
					ud = AuthzUtils.getUserDetails(ownerProperty.getValue());
					if (ud == null) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			boolean deletable = true;
			String expression = getReadonly().getExpression();
			if (ownerProperty != null || StringUtils.isNotBlank(expression)) {
				for (Serializable uid : id) {
					Persistable<?> en = entityManager.get(uid);
					if (en == null)
						continue;
					if (ownerProperty != null) {
						Owner owner = ownerProperty.getKey();
						if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
								.authorize(null, owner.supervisorRole(), null))
								&& (owner.isolate() || owner.readonlyForOther())) {
							BeanWrapperImpl bwi = new BeanWrapperImpl(en);
							bwi.setConversionService(conversionService);
							Object value = bwi.getPropertyValue(owner
									.propertyName());
							if (value == null || !ud.equals(value)) {
								addActionError(getText("delete.forbidden",
										new String[] { en.toString() }));
								deletable = false;
								break;
							}
						}
					}
					if (StringUtils.isNotBlank(expression)
							&& checkEntityReadonly(expression, en)
							&& !getReadonly().isDeletable()) {
						addActionError(getText("delete.forbidden",
								new String[] { en.toString() }));
						deletable = false;
						break;
					}
				}
			}
			if (deletable) {
				List<Persistable<?>> list = entityManager.delete(id);
				if (list.size() > 0)
					addActionMessage(getText("delete.success"));
			}
		}
		return SUCCESS;
	}

	public String enable() {
		if (!isEnableable() || getReadonly().isValue())
			return ACCESSDENIED;
		return updateEnabled(true);
	}

	public String disable() {
		if (!isEnableable() || getReadonly().isValue())
			return ACCESSDENIED;
		return updateEnabled(false);
	}

	protected String updateEnabled(boolean enabled) {
		BaseManager<Persistable<?>> em = getEntityManager(getEntityClass());
		String[] arr = getId();
		Serializable[] id = (arr != null) ? new Serializable[arr.length]
				: new Serializable[0];
		try {
			BeanWrapperImpl bw = new BeanWrapperImpl(getEntityClass()
					.newInstance());
			bw.setConversionService(conversionService);
			for (int i = 0; i < id.length; i++) {
				bw.setPropertyValue("id", arr[i]);
				id[i] = (Serializable) bw.getPropertyValue("id");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		if (id.length > 0) {
			Tuple<Owner, Class<? extends UserDetails>> ownerProperty = getOwnerProperty();
			UserDetails ud = null;
			if (ownerProperty != null) {
				Owner owner = ownerProperty.getKey();
				if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
						.authorize(null, owner.supervisorRole(), null))) {
					ud = AuthzUtils.getUserDetails(ownerProperty.getValue());
					if (ud == null) {
						addActionError(getText("access.denied"));
						return ACCESSDENIED;
					}
				}
			}
			for (Serializable s : id) {
				Enableable en = (Enableable) em.get(s);
				if (en == null || en.isEnabled() == enabled)
					continue;
				if (ownerProperty != null) {
					Owner owner = ownerProperty.getKey();
					if (!(StringUtils.isNotBlank(owner.supervisorRole()) && AuthzUtils
							.authorize(null, owner.supervisorRole(), null))
							&& (owner.isolate() || owner.readonlyForOther())) {
						BeanWrapperImpl bwi = new BeanWrapperImpl(en);
						bwi.setConversionService(conversionService);
						Object value = bwi.getPropertyValue(owner
								.propertyName());
						if (value == null || !ud.equals(value))
							continue;
					}
				}
				String expression = getReadonly().getExpression();
				if (StringUtils.isNotBlank(expression)
						&& checkEntityReadonly(expression, (Persistable<?>) en))
					continue;
				en.setEnabled(enabled);
				em.save((Persistable) en);
			}
			addActionMessage(getText("operate.success"));
		}
		return SUCCESS;
	}

	@Override
	protected Authorize findAuthorize() {
		Authorize authorize = super.findAuthorize();
		if (authorize == null) {
			Class<?> c = getEntityClass();
			return c.getAnnotation(Authorize.class);
		}
		return authorize;
	}

	// need call once before view
	protected Class<Persistable<?>> getEntityClass() {
		if (entityClass == null)
			entityClass = (Class<Persistable<?>>) ReflectionUtils
					.getGenericClass(getClass());
		if (entityClass == null) {
			ActionProxy proxy = ActionContext.getContext()
					.getActionInvocation().getProxy();
			String actionName = getEntityName();
			String namespace = proxy.getNamespace();
			entityClass = (Class<Persistable<?>>) AutoConfigPackageProvider
					.getEntityClass(namespace, actionName);
		}
		return entityClass;
	}

	private Class<Persistable<?>> entityClass;

	private void putEntityToValueStack(Persistable entity) {
		ValueStack vs = ActionContext.getContext().getValueStack();
		if (entity != null)
			vs.set(getEntityName(), entity);
	}

	protected Persistable constructEntity() {
		Persistable entity = null;
		try {
			entity = getEntityClass().newInstance();
			ValueStack temp = valueStackFactory.createValueStack();
			temp.set(getEntityName(), entity);
			Map<String, Object> context = temp.getContext();
			Map<String, Object> parameters = ActionContext.getContext()
					.getParameters();
			try {
				ReflectionContextState.setCreatingNullObjects(context, true);
				ReflectionContextState.setDenyMethodExecution(context, true);
				for (Map.Entry<String, Object> entry : parameters.entrySet()) {
					String name = entry.getKey();
					if (name.startsWith(getEntityName() + "."))
						temp.setParameter(name, entry.getValue());
				}
			} finally {
				ReflectionContextState.setCreatingNullObjects(context, false);
				ReflectionContextState.setDenyMethodExecution(context, false);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return entity;
	}

	@Inject
	private transient ValueStackFactory valueStackFactory;

	@Inject
	private transient FreemarkerManager freemarkerManager;

}