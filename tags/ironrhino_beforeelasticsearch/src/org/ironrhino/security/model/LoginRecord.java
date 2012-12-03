package org.ironrhino.security.model;

import java.util.Date;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;

@AutoConfig(readonly = true, order = "date desc")
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class LoginRecord extends BaseEntity {

	private static final long serialVersionUID = -7691080078972338500L;

	@UiConfig(displayOrder = 1)
	private String username;

	@UiConfig(displayOrder = 2)
	private String address;

	@UiConfig(displayOrder = 3)
	private boolean failed;

	@UiConfig(displayOrder = 4)
	private String cause;

	@UiConfig(displayOrder = 5, width = "150px")
	private Date date = new Date();

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}