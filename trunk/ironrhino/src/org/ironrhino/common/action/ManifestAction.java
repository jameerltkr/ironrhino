package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.Constants;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;

@AutoConfig(namespace = "/")
public class ManifestAction extends BaseAction {

	private static final long serialVersionUID = -5865373753326653067L;

	@Inject
	private SettingControl settingControl;

	private String version;

	private List<String> caches;

	private Map<String, String> fallbacks;

	private List<String> networks;

	public String getVersion() {
		return version;
	}

	public List<String> getCaches() {
		return caches;
	}

	public Map<String, String> getFallbacks() {
		return fallbacks;
	}

	public List<String> getNetworks() {
		return networks;
	}

	@Override
	public String execute() {
		caches = new ArrayList<String>();
		caches.add("test");
		networks = new ArrayList<String>();
		networks.add("*");
		version = settingControl
				.getStringValue(Constants.SETTING_KEY_MANIFEST_VERSION);
		if (StringUtils.isBlank(version)) {
			// TODO calculate version
		}

		ServletActionContext.getResponse()
				.setContentType("text/cache-manifest");
		return "manifest";
	}

}
