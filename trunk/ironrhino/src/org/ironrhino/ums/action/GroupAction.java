package org.ironrhino.ums.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ecside.common.util.RequestUtil;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.ResultPage;
import org.ironrhino.core.ext.struts.BaseAction;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.ums.model.Group;
import org.ironrhino.ums.model.Role;
import org.ironrhino.ums.service.GroupManager;
import org.ironrhino.ums.service.RoleManager;

import com.opensymphony.xwork2.validator.annotations.RegexFieldValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validation;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

@Validation
public class GroupAction extends BaseAction {

	private Group group;

	private ResultPage<Group> resultPage;

	private String rolesAsString;

	private GroupManager groupManager;

	private RoleManager roleManager;

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public ResultPage<Group> getResultPage() {
		return resultPage;
	}

	public void setResultPage(ResultPage<Group> resultPage) {
		this.resultPage = resultPage;
	}

	public String getRolesAsString() {
		return rolesAsString;
	}

	public void setRolesAsString(String rolesAsString) {
		this.rolesAsString = rolesAsString;
	}

	public void setGroupManager(GroupManager groupManager) {
		this.groupManager = groupManager;
	}

	public void setRoleManager(RoleManager roleManager) {
		this.roleManager = roleManager;
	}

	public String execute() {
		HttpServletRequest request = ServletActionContext.getRequest();
		DetachedCriteria dc = groupManager.detachedCriteria();
		if (group != null) {
			if (StringUtils.isNotBlank(group.getName()))
				dc.add(Restrictions.ilike("name", group.getName(),
						MatchMode.ANYWHERE));
			if (StringUtils.isNotBlank(group.getDescription()))
				dc.add(Restrictions.ilike("description",
						group.getDescription(), MatchMode.ANYWHERE));
			String value = request.getParameter("group.enabled");
			if ("true".equals(value))
				dc.add(Restrictions.eq("enabled", true));
			else if ("false".equals(value))
				dc.add(Restrictions.eq("enabled", false));
		}
		if (resultPage == null)
			resultPage = new ResultPage<Group>();
		resultPage.setDetachedCriteria(dc);
		resultPage.addOrder(Order.asc("name"));
		int totalRows = groupManager.countResultPage(resultPage);
		String pageSize = request.getParameter("ec_rd");
		if (StringUtils.isNumeric(pageSize))
			resultPage.setPageSize(Integer.parseInt(pageSize));
		int[] rowStartEnd = RequestUtil.getRowStartEnd(request, totalRows,
				resultPage.getPageSize());
		resultPage.setStart(rowStartEnd[0]);
		resultPage = groupManager.getResultPage(resultPage);
		request.setAttribute("recordList", resultPage.getResult());
		request.setAttribute("totalRows", resultPage.getTotalRecord());
		return "list";
	}

	public String input() {
		group = groupManager.get(getUid());
		if (group == null)
			group = new Group();
		return INPUT;
	}

	@Validations(requiredStrings = { @RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "group.name", trim = true, key = "group.name.required", message = "请输入名字") }, regexFields = { @RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "group.name", expression = "^\\w+$", key = "group.name.invalid", message = "必须为数字或者字母或者下划线") })
	public String save() {
		if (group.isNew()) {
			group.setName(group.getName().toUpperCase());
			if (groupManager.getByNaturalId("name", group.getName()) != null) {
				addFieldError("group.name", getText("role.name.exists"));
				return INPUT;
			}
		} else {
			Group temp = group;
			group = groupManager.get(temp.getId());
			BeanUtils.copyProperties(temp, group);
			if (rolesAsString != null) {
				group.getRoles().clear();
				groupManager.save(group);
				String[] array = StringUtils.split(rolesAsString.toUpperCase(),
						",");
				Set<String> set = new HashSet<String>();
				for (String name : array) {
					name = name.trim();
					if (!"".equals(name))
						set.add(name);
				}
				for (String name : set) {
					Role role = roleManager.getByNaturalId("name", name);
					if (role != null)
						group.getRoles().add(role);
				}
			}
		}
		groupManager.save(group);
		addActionMessage(getText("save.success", "save {0} successfully",
				new String[] { group.getName() }));
		return SUCCESS;
	}

	public String delete() {
		String[] id = getId();
		if (id != null) {
			DetachedCriteria dc = groupManager.detachedCriteria();
			dc.add(Restrictions.in("id", id));
			List<Group> list = groupManager.getListByCriteria(dc);
			if (list.size() > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append("(");
				for (Group group : list) {
					groupManager.delete(group);
					sb.append(group.getName() + ",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(")");
				addActionMessage(getText("delete.success",
						"delete {0} successfully",
						new String[] { sb.toString() }));
			}
		}
		return SUCCESS;
	}
}
