package org.ironrhino.ums.model;

import java.util.HashSet;
import java.util.Set;

import org.ironrhino.core.annotation.AutoConfig;
import org.ironrhino.core.annotation.NaturalId;
import org.ironrhino.core.annotation.NotInCopy;
import org.ironrhino.core.annotation.Recordable;
import org.ironrhino.core.model.BaseEntity;


@Recordable
@AutoConfig
public class Role extends BaseEntity {

	@NaturalId
	private String name;

	private String description;

	private boolean enabled;

	@NotInCopy
	private Set<Group> groups = new HashSet<Group>(0);

	public Role() {
		this.enabled = true;
	}

	public Role(String name) {
		this();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public void setGroups(Set<Group> groups) {
		this.groups = groups;
	}
	
	public String toString(){
		return this.name;
	}
}
