package org.ironrhino.core.struts;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.opensymphony.module.sitemesh.Config;
import com.opensymphony.module.sitemesh.Decorator;
import com.opensymphony.module.sitemesh.DecoratorMapper;
import com.opensymphony.module.sitemesh.Page;
import com.opensymphony.module.sitemesh.mapper.AbstractDecoratorMapper;

public class RequestDecoratorMapper extends AbstractDecoratorMapper {

	private ServletContext sc;

	private String decoratorParameter = "decorator";

	public void init(Config config, Properties properties,
			DecoratorMapper parent) throws InstantiationException {
		super.init(config, properties, parent);
		sc = config.getServletContext();
		sc.setAttribute(this.getClass().getName(), this);
		decoratorParameter = properties.getProperty("decorator.parameter",
				"decorator");
	}

	public Decorator getDecorator(HttpServletRequest request, Page page) {
		Decorator result = null;
		String decorator = (String) request.getAttribute(decoratorParameter);

		if (decorator != null) {
			result = getNamedDecorator(request, decorator);
		}
		return result == null ? super.getDecorator(request, page) : result;
	}

	public void setDecorator(HttpServletRequest request, String name) {
		Decorator result = getNamedDecorator(request, name);
		if (result != null)
			request.setAttribute(decoratorParameter, name);
	}

	public static void setDecorator(String name) {
		RequestDecoratorMapper rdm = (RequestDecoratorMapper) ServletActionContext
				.getServletContext().getAttribute(
						RequestDecoratorMapper.class.getName());
		if (rdm != null)
			rdm.setDecorator(ServletActionContext.getRequest(), name);
	}
}