package org.ironrhino.core.aop;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.mvel2.templates.TemplateRuntime;
import org.springframework.core.Ordered;

public class BaseAspect implements Ordered {

	protected Log log = LogFactory.getLog(getClass());

	private int order;

	protected boolean isBypass() {
		return AopContext.isBypass(this.getClass());
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	protected Object eval(String template, JoinPoint jp, Object retval) {
		if (template == null)
			return null;
		template = template.trim();
		if (template.length() == 0)
			return "";
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("_this", jp.getThis());
		context.put("target", jp.getTarget());
		context.put("aspect", this);
		if (retval != null)
			context.put("retval", retval);
		context.put("args", jp.getArgs());
		return TemplateRuntime.eval(template, context);
	}

	protected Object eval(String template, JoinPoint jp) {
		return eval(template, jp, null);
	}

	protected String evalString(String template, JoinPoint jp, Object retval) {
		try {
			Object obj = eval(template, jp, retval);
			if (obj == null)
				return null;
			return obj.toString();
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			return template;
		}
	}

	protected boolean evalBoolean(String template, JoinPoint jp, Object retval) {
		try {
			if (StringUtils.isBlank(template))
				return true;
			return Boolean.parseBoolean(evalString(template, jp, retval));
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	protected int evalInt(String template, JoinPoint jp, Object retval) {
		try {
			Object obj = eval(template, jp, retval);
			if (obj == null)
				return 0;
			if (obj instanceof Integer)
				return (Integer) obj;
			return Integer.parseInt(obj.toString());
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			return 0;
		}

	}

	protected long evalLong(String template, JoinPoint jp, Object retval) {
		try {
			Object obj = eval(template, jp, retval);
			if (obj == null)
				return 0;
			if (obj instanceof Long)
				return (Long) obj;
			return Long.parseLong(obj.toString());
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			return 0;
		}

	}

	protected double evalDouble(String template, JoinPoint jp, Object retval) {
		try {
			Object obj = eval(template, jp, retval);
			if (obj == null)
				return 0;
			if (obj instanceof Double)
				return (Double) obj;
			return Double.parseDouble(obj.toString());
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			return 0;
		}

	}

	protected List evalList(String template, JoinPoint jp, Object retval) {
		try {
			Object obj = eval(template, jp, retval);
			if (obj == null)
				return null;
			if (obj instanceof List)
				return (List) obj;
			return Arrays.asList(obj.toString().split(","));
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
}
