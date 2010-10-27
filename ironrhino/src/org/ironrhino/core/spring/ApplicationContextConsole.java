package org.ironrhino.core.spring;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.event.SetPropertyEvent;
import org.ironrhino.core.metadata.PostPropertiesReset;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ExpressionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.support.PropertiesLoaderSupport;

@Singleton
@Named("applicationContextConsole")
public class ApplicationContextConsole implements ApplicationListener {

	private static final Pattern SET_PROPERTY_EXPRESSION_PATTERN = Pattern
			.compile("(^[a-zA-Z][a-zA-Z0-9_\\-]*\\.[a-zA-Z][a-zA-Z0-9_\\-]*\\s*=\\s*.+$)");

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Inject
	private ApplicationContext ctx;

	@Autowired(required = false)
	private ServletContext servletContext;

	@Inject
	private EventPublisher eventPublisher;

	private Map<String, Object> beans = new HashMap<String, Object>();

	@Autowired(required = false)
	private PropertyPlaceholderConfigurer propertyPlaceholderConfigurer;

	private Properties properties;

	public Map<String, Object> getBeans() {
		if (beans.isEmpty()) {
			if (servletContext != null)
				beans.put(
						"freemarkerConfiguration",
						servletContext
								.getAttribute(FreemarkerManager.CONFIG_SERVLET_CONTEXT_KEY));
			String[] beanNames = ctx.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				if (StringUtils.isAlphanumeric(beanName)
						&& ctx.isSingleton(beanName))
					beans.put(beanName, ctx.getBean(beanName));
			}
			try {
				if (propertyPlaceholderConfigurer != null) {
					Method method = PropertiesLoaderSupport.class
							.getDeclaredMethod("mergeProperties", new Class[0]);
					method.setAccessible(true);
					properties = (Properties) method.invoke(
							propertyPlaceholderConfigurer, new Object[0]);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return beans;
	}

	public Object execute(String expression) throws Exception {
		try {
			if (isSetProperyExpression(expression)) {
				executeSetProperty(expression, true);
				return null;
			} else {
				return executeMethodInvocation(expression);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	private Object executeMethodInvocation(String expression) throws Exception {
		try {
			return ExpressionUtils.evalExpression(expression, getBeans());
		} catch (Exception e) {
			throw e;
		}
	}

	private void executeSetProperty(String expression, boolean global)
			throws Exception {
		if (global) {
			eventPublisher.publish(new SetPropertyEvent(expression), true);
		} else {
			try {
				Object bean = null;
				if (expression.indexOf('=') > 0) {
					bean = getBeans().get(
							expression.substring(0, expression.indexOf('.')));
				}
				ExpressionUtils.evalExpression(expression, getBeans());
				if (bean != null) {
					Method m = AnnotationUtils.getAnnotatedMethod(
							bean.getClass(), PostPropertiesReset.class);
					if (m != null)
						m.invoke(bean, new Object[0]);
				}
			} catch (Exception e) {
				throw e;
			}
		}
	}

	public String getConfigValue(String key) {
		String value = null;
		if (properties != null)
			value = properties.getProperty(key);
		if (value == null)
			value = System.getProperty(key);
		return value;
	}

	private static boolean isSetProperyExpression(String expression) {
		Matcher matcher = SET_PROPERTY_EXPRESSION_PATTERN.matcher(expression);
		return matcher.matches();
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof SetPropertyEvent) {
			String expression = ((SetPropertyEvent) event).getExpression();
			try {
				executeSetProperty(expression, false);
			} catch (Exception e) {
				log.error("execute '" + expression + "' error", e);
			}
		}
	}

}