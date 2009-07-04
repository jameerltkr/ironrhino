package org.ironrhino.common.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.tagext.Tag;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.model.SimpleElement;
import org.ironrhino.core.model.Secured;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.HttpSessionContextIntegrationFilter;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.taglibs.authz.AuthorizeTag;
import org.springframework.security.userdetails.UserDetails;

public class AuthzUtils {

	public static Object authentication(String property) {
		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (auth.getPrincipal() == null)
			return null;
		try {
			BeanWrapperImpl wrapper = new BeanWrapperImpl(auth);
			return wrapper.getPropertyValue(property);
		} catch (BeansException e) {
			return null;
		}
	}

	public static boolean authorize(String ifAllGranted, String ifAnyGranted,
			String ifNotGranted) {
		try {
			AuthorizeTag tag = new AuthorizeTag();
			tag.setIfAllGranted(ifAllGranted);
			tag.setIfAnyGranted(ifAnyGranted);
			tag.setIfNotGranted(ifNotGranted);
			return tag.doStartTag() == Tag.EVAL_BODY_INCLUDE;
		} catch (Exception e) {
			return false;
		}
	}

	public static List<String> getRoleNames() {
		List<String> roleNames = new ArrayList<String>();
		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			GrantedAuthority[] authz = SecurityContextHolder.getContext()
					.getAuthentication().getAuthorities();
			if (authz != null)
				for (GrantedAuthority var : authz)
					roleNames.add(var.getAuthority());
		}
		return roleNames;
	}

	public static boolean hasPermission(Secured entity) {
		if (entity == null)
			return false;
		if (entity.getRoles() == null || entity.getRoles().size() == 0)
			return true;
		List<String> roleNames = getRoleNames();
		for (SimpleElement n : entity.getRoles()) {
			if (roleNames.contains(n.getValue()))
				return true;
		}
		return false;
	}

	public static String getUsername() {
		SecurityContext sc = SecurityContextHolder.getContext();
		if (sc == null)
			return null;
		Authentication auth = sc.getAuthentication();
		if (auth == null)
			return null;
		return auth.getName();
	}

	public static <T extends UserDetails> T getUserDetails(Class<T> clazz) {
		SecurityContext sc = SecurityContextHolder.getContext();
		if (sc == null)
			return null;
		Authentication auth = sc.getAuthentication();
		if (auth == null)
			return null;
		Object principal = auth.getPrincipal();
		if (principal == null)
			return null;
		if (clazz.isAssignableFrom(principal.getClass()))
			return (T) principal;
		return null;
	}

	public static void autoLogin(UserDetails ud) {
		SecurityContext sc = new SecurityContextImpl();
		Authentication auth = new UsernamePasswordAuthenticationToken(ud, ud
				.getPassword(), ud.getAuthorities());
		sc.setAuthentication(auth);
		ServletActionContext
				.getRequest()
				.getSession()
				.setAttribute(
						HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY,
						sc);
	}
}