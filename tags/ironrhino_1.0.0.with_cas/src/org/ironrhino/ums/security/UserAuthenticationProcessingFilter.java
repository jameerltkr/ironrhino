package org.ironrhino.ums.security;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.aop.CacheAspect;
import org.ironrhino.core.spring.security.BaseAuthenticationProcessingFilter;
import org.ironrhino.ums.model.LoginRecord;
import org.ironrhino.ums.model.User;
import org.ironrhino.ums.service.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;

public class UserAuthenticationProcessingFilter extends
		BaseAuthenticationProcessingFilter {

	@Autowired
	private UserManager userManager;

	@Override
	protected void onSuccessfulAuthentication(HttpServletRequest request,
			HttpServletResponse response, Authentication authResult)
			throws IOException {
		super.onSuccessfulAuthentication(request, response, authResult);
		User user = (User) authResult.getPrincipal();
		user.setLoginTimes(user.getLoginTimes() + 1);
		user.setLastLoginDate(new Date());
		user.setLastLoginAddress(request.getRemoteAddr());
		AopContext.setBypass(CacheAspect.class);
		userManager.save(user);
		LoginRecord loginRecord = new LoginRecord();
		loginRecord.setUsername(user.getUsername());
		loginRecord.setLoginAddress(request.getRemoteAddr());
		save(loginRecord);
	}

	@Override
	protected void onUnsuccessfulAuthentication(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException failed)
			throws IOException {
		LoginRecord loginRecord = new LoginRecord();
		loginRecord.setUsername((String) request.getSession().getAttribute(
				SPRING_SECURITY_LAST_USERNAME_KEY));
		loginRecord.setLoginAddress(request.getRemoteAddr());
		loginRecord.setFailed(true);
		loginRecord.setCause(failed.getMessage());
		save(loginRecord);
	}

	private void save(final LoginRecord loginRecord) {
		userManager.execute(new HibernateCallback() {
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				session.save(loginRecord);
				return null;
			}
		});
	}

}