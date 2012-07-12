package org.ironrhino.core.spring.security;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.RedirectUrlBuilder;

public class DefaultLoginUrlAuthenticationEntryPoint extends
		LoginUrlAuthenticationEntryPoint {

	@Value("${sso.server.base:}")
	private String ssoServerBase;

	public void setSsoServerBase(String ssoServerBase) {
		this.ssoServerBase = ssoServerBase;
	}

	@Override
	protected String buildRedirectUrlToLoginPage(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException authException) {
		String targetUrl = null;
		String redirectUrl = null;
		SavedRequest savedRequest = (SavedRequest) request.getSession()
				.getAttribute(WebAttributes.SAVED_REQUEST);
		request.getSession().removeAttribute(WebAttributes.SAVED_REQUEST);
		if (savedRequest != null) {
			if (savedRequest instanceof DefaultSavedRequest) {
				DefaultSavedRequest dsr = (DefaultSavedRequest) savedRequest;
				String queryString = dsr.getQueryString();
				if (StringUtils.isBlank(queryString)) {
					targetUrl = dsr.getRequestURL();
				} else {
					targetUrl = new StringBuilder(dsr.getRequestURL()).append(
							"?").append(queryString).toString();
				}
			} else
				targetUrl = savedRequest.getRedirectUrl();
		}
		StringBuilder loginUrl = new StringBuilder();
		if (StringUtils.isBlank(ssoServerBase)) {
			URL url = null;
			try {
				url = new URL(request.getRequestURL().toString());
			} catch (MalformedURLException e) {

			}
			RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
			String scheme = request.getScheme();
			int serverPort = getPortResolver().getServerPort(request);
			urlBuilder.setScheme(scheme);
			urlBuilder.setServerName(url.getHost());
			urlBuilder.setPort(serverPort);
			urlBuilder.setContextPath(request.getContextPath());
			urlBuilder.setPathInfo(getLoginFormUrl());
			if (isForceHttps() && "http".equals(scheme)) {
				Integer httpsPort = getPortMapper().lookupHttpsPort(serverPort);
				urlBuilder.setScheme("https");
				urlBuilder.setPort(httpsPort);
			}
			loginUrl = new StringBuilder(urlBuilder.getUrl());
		} else {
			loginUrl = new StringBuilder(ssoServerBase)
					.append(getLoginFormUrl());
		}
		try {
			if (StringUtils.isNotBlank(targetUrl))
				loginUrl.append('?').append(
						DefaultUsernamePasswordAuthenticationFilter.TARGET_URL)
						.append('=').append(
								URLEncoder.encode(targetUrl, "UTF-8"));
			redirectUrl = loginUrl.toString();
			if (isForceHttps() && redirectUrl.startsWith("http://")) {
				URL url = new URL(redirectUrl);
				RedirectUrlBuilder urlBuilder = new RedirectUrlBuilder();
				urlBuilder.setScheme("https");
				urlBuilder.setServerName(url.getHost());
				Integer httpsPort = getPortMapper().lookupHttpsPort(
						url.getPort());
				urlBuilder.setPort(httpsPort);
				urlBuilder.setPathInfo(url.getPath());
				urlBuilder.setQuery(url.getQuery());
				redirectUrl = urlBuilder.getUrl();
			}
		} catch (Exception e) {
			redirectUrl = loginUrl.toString();
		}
		return redirectUrl;
	}
}