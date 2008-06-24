package org.ironrhino.online.support;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.common.support.TemplateProvider;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.pms.model.Product;
import org.ironrhino.pms.service.ProductManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ResourceLoader;

import freemarker.template.Template;

public class ProductPageGenerator implements ApplicationListener {

	protected final Log log = LogFactory.getLog(ProductPageGenerator.class);

	private int batchSize = 50;

	private boolean enabled = false;

	private String siteBaseUrl;

	private String templateName = "product.ftl";

	private String directory = "/product";

	private ProductManager productManager;

	private TemplateProvider templateProvider;

	@Autowired
	private ResourceLoader resourceLoader;

	private Template template;

	public String getSiteBaseUrl() {
		return siteBaseUrl;
	}

	public void setSiteBaseUrl(String siteBaseUrl) {
		this.siteBaseUrl = siteBaseUrl;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@Required
	public void setProductManager(ProductManager productManager) {
		this.productManager = productManager;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	@Required
	public void setTemplateProvider(TemplateProvider templateProvider) {
		this.templateProvider = templateProvider;
	}

	@PostConstruct
	public void afterPropertiesSet() throws Exception {
		template = templateProvider.getTemplate(templateName);
	}

	public synchronized void generate() throws IOException {
		if (!enabled)
			return;
		int totalRecord = productManager.countAll();
		int totalPage = totalRecord % batchSize == 0 ? totalRecord / batchSize
				: totalRecord / batchSize + 1;
		long time = System.currentTimeMillis();
		for (int i = 1; i <= totalPage; i++)
			generate(i);
		log.info("generated " + totalRecord + " product page in "
				+ (System.currentTimeMillis() - time) + "ms");
	}

	private void generate(int pageNo) throws IOException {
		DetachedCriteria dc = productManager.detachedCriteria();
		dc.setFetchMode("attributes", FetchMode.JOIN);
		dc.setFetchMode("relatedProducts", FetchMode.JOIN);
		List<Product> products = productManager.getListByCriteria(dc, pageNo,
				batchSize);
		for (Product product : products)
			generate(product);
		productManager.clear();
	}

	// use freemarker template
	public void generate(Product product) throws IOException {
		Writer out = null;
		try {
			out = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(new File(
							resourceLoader.getResource(directory).getFile(),
							product.getCode() + ".html")), "UTF-8"));
			Map<String, Product> model = new HashMap<String, Product>(1);
			model.put("product", product);
			template.process(model, out);
		} catch (Exception ex) {
			log.error("Error when generate static page for product["
					+ product.getCode() + "]", ex);
		} finally {
			if (out != null)
				out.close();
		}
	}

	// use jsp template
	public void generate2(Product product) throws IOException {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			URL url = new URL(siteBaseUrl + "/product/view/"
					+ product.getCode());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.connect();
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
				return;
			reader = new BufferedReader(new InputStreamReader(conn
					.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(new File(
							resourceLoader.getResource(directory).getFile(),
							product.getCode() + ".html")), "UTF-8"));
			String line = null;
			while ((line = reader.readLine()) != null)
				writer.write(line + "\n");
		} catch (Exception ex) {
			log.error("Error when generate static page for product["
					+ product.getCode() + "]", ex);
		} finally {
			if (reader != null)
				reader.close();
			if (writer != null)
				writer.close();
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (!enabled)
			return;
		if (templateProvider == null)
			return;
		if (event instanceof EntityOperationEvent) {
			EntityOperationEvent ev = (EntityOperationEvent) event;
			if (ev.getEntity() instanceof Product) {
				Product product = (Product) ev.getEntity();
				if (ev.getType() == EntityOperationType.CREATE
						|| ev.getType() == EntityOperationType.UPDATE)
					try {
						generate(product);
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
			}
		}
	}

}
