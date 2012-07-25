package org.ironrhino.security.socialauth.impl;

import javax.inject.Named;
import javax.inject.Singleton;

import org.compass.core.util.reader.StringReader;
import org.ironrhino.core.util.XmlUtils;
import org.ironrhino.security.socialauth.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Named("douban")
@Singleton
public class Douban extends OAuth10aProvider {

	@Value("${douban.requestTokenUrl:http://www.douban.com/service/auth/request_token}")
	private String requestTokenUrl;

	@Value("${douban.authorizeUrl:http://www.douban.com/service/auth/authorize}")
	private String authorizeUrl;

	@Value("${douban.accessTokenUrl:http://www.douban.com/service/auth/access_token}")
	private String accessTokenUrl;

	@Value("${douban.logo:http://img3.douban.com/pics/nav/lg_main_a6.png}")
	private String logo;

	public String getLogo() {
		return logo;
	}

	public String getRequestTokenUrl() {
		return requestTokenUrl;
	}

	public String getAuthorizeUrl() {
		return authorizeUrl;
	}

	public String getAccessTokenUrl() {
		return accessTokenUrl;
	}

	@Override
	protected Profile doGetProfile(OAuth10aToken token) throws Exception {
		String xml = invoke(token,
				"http://api.douban.com/people/%40me");
		Document doc = XmlUtils.getDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		String uid = doc.getElementsByTagName("db:uid").item(0)
				.getTextContent();
		String displayName = doc.getElementsByTagName("title").item(0)
				.getTextContent();
		Profile p = new Profile();
		p.setId(generateId(uid));
		p.setDisplayName(displayName);
		return p;
	}

}