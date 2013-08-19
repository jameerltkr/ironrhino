package org.ironrhino.security.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.ReadonlyConfig;
import org.ironrhino.core.metadata.RichtableConfig;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Searchable(type = "loginRecord")
@Table(name = "loginrecord")
@RichtableConfig(searchable = true, order = "date desc", readonlyConfig = @ReadonlyConfig(true))
public class LoginRecord extends BaseEntity {

	private static final long serialVersionUID = -7691080078972338500L;

	@UiConfig(displayOrder = 1, width = "100px")
	@Column(nullable = false)
	@SearchableProperty
	private String username;

	@UiConfig(displayOrder = 2, width = "100px")
	@SearchableProperty
	private String address;

	@UiConfig(displayOrder = 3, width = "80px")
	private boolean failed;

	@UiConfig(displayOrder = 4)
	@SearchableProperty
	private String cause;

	@UiConfig(displayOrder = 5, width = "150px")
	@Column(name = "`date`")
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
