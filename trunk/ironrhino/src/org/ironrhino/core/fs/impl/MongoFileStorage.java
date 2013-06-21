package org.ironrhino.core.fs.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.fs.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.google.common.io.Files;

@Singleton
@Named("fileStorage")
@Profile({ CLOUD, CLUSTER })
public class MongoFileStorage implements FileStorage {

	@Value("${fileStorage.uri:file}")
	protected String uri;

	@Inject
	private MongoTemplate mongoTemplate;

	@PostConstruct
	public void afterPropertiesSet() {
		if (!mongoTemplate.collectionExists(File.class))
			mongoTemplate.createCollection(File.class);
	}

	public void write(InputStream is, String path) throws IOException {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			throw new IOException("cannot direct access path /");
		File file = mongoTemplate.findById(path, File.class);
		if (file == null) {
			int lastIndex = path.lastIndexOf('/');
			if (lastIndex > 0) {
				int index = 0;
				while (index < lastIndex) {
					index = path.indexOf('/', index + 1);
					if (index < 0)
						break;
					String parent = path.substring(0, index);
					File parentFile = mongoTemplate
							.findById(parent, File.class);
					if (parentFile == null) {
						parentFile = new File();
						parentFile.setPath(parent);
						parentFile.setDirectory(true);
						parentFile.setLastModified(System.currentTimeMillis());
						mongoTemplate.save(parentFile);
					} else if (!parentFile.isDirectory())
						throw new IOException("parent " + parent
								+ " is not directory while writing path "
								+ path);
				}
			}
			file = new File();
			file.setPath(path);
		} else if (file.isDirectory())
			throw new IOException("path " + path
					+ " is directory,can not be written");
		ByteArrayOutputStream os = new ByteArrayOutputStream(512 * 1024);
		IOUtils.copy(is, os);
		file.setData(os.toByteArray());
		file.setLastModified(System.currentTimeMillis());
		os.close();
		is.close();
		mongoTemplate.save(file);
	}

	public InputStream open(String path) throws IOException {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			throw new IOException("cannot direct access path /");
		File file = mongoTemplate.findById(path, File.class);
		if (file == null)
			throw new IOException("path " + path + " doesn't exists");
		if (file.isDirectory())
			throw new IOException("path " + path + " is directory");
		return new ByteArrayInputStream(file.getData());
	}

	public boolean mkdir(String path) {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			return true;
		File file = mongoTemplate.findById(path, File.class);
		if (file != null)
			return false;
		int lastIndex = path.lastIndexOf('/');
		if (lastIndex > 0) {
			int index = 0;
			while (index <= lastIndex) {
				index = path.indexOf('/', index + 1);
				if (index < 0)
					break;
				String parent = path.substring(0, index);
				File parentFile = mongoTemplate.findById(parent, File.class);
				if (parentFile == null) {
					parentFile = new File();
					parentFile.setPath(parent);
					parentFile.setDirectory(true);
					parentFile.setLastModified(System.currentTimeMillis());
					mongoTemplate.save(parentFile);
				} else if (!parentFile.isDirectory())
					return false;
			}
		}
		file = new File();
		file.setPath(path);
		file.setDirectory(true);
		file.setLastModified(System.currentTimeMillis());
		mongoTemplate.save(file);
		return true;
	}

	public boolean delete(String path) {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			return false;
		File file = mongoTemplate.findById(path, File.class);
		if (file == null)
			return false;
		if (file.isDirectory()) {
			int size = mongoTemplate
					.find(new Query(where("path").regex(
							"^" + path.replaceAll("\\.", "\\\\.") + "/.*"))
							.limit(1),
							File.class).size();
			if (size > 0)
				return false;
		}
		mongoTemplate.remove(file);
		return true;
	}

	public long getLastModified(String path) {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			return -1;
		File file = mongoTemplate.findById(path, File.class);
		return file != null ? file.getLastModified() : -1;
	}

	public boolean exists(String path) {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			return true;
		return mongoTemplate.findById(path, File.class) != null;
	}

	public boolean rename(String fromPath, String toPath) {
		fromPath = Files.simplifyPath(fromPath);
		toPath = Files.simplifyPath(toPath);
		if (fromPath.equals("/") || toPath.equals("/"))
			return false;
		String s1 = fromPath.substring(0, fromPath.lastIndexOf('/'));
		String s2 = toPath.substring(0, fromPath.lastIndexOf('/'));
		if (!s1.equals(s2))
			return false;
		File fromfile = mongoTemplate.findById(fromPath, File.class);
		if (fromfile == null)
			return false;
		File tofile = mongoTemplate.findById(toPath, File.class);
		if (tofile == null) {
			tofile = new File();
			tofile.setPath(toPath);
		}
		tofile.setData(fromfile.getData());
		tofile.setLastModified(fromfile.getLastModified());
		mongoTemplate.save(tofile);
		mongoTemplate.remove(fromfile);
		return true;
	}

	public boolean isDirectory(String path) {
		path = Files.simplifyPath(path);
		if (path.equals("/"))
			return true;
		File file = mongoTemplate.findById(path, File.class);
		return file != null && file.isDirectory();
	}

	public List<String> listFiles(String path) {
		path = Files.simplifyPath(path);
		File file = mongoTemplate.findById(path, File.class);
		if (file == null || !file.isDirectory())
			return null;
		List<String> list = new ArrayList<String>();
		List<File> files = mongoTemplate.find(
				new Query(where("path").regex(
						"^" + path.replaceAll("\\.", "\\\\.") + "/[^/]*$")),
				File.class);
		for (File f : files) {
			if (f.isDirectory())
				continue;
			String name = f.getPath();
			list.add(name.substring(name.lastIndexOf('/') + 1));
		}
		Collections.sort(list);
		return list;
	}

	public Map<String, Boolean> listFilesAndDirectory(String path) {
		path = Files.simplifyPath(path);
		final Map<String, Boolean> map = new HashMap<String, Boolean>();
		File file = mongoTemplate.findById(path, File.class);
		if (file == null || !file.isDirectory())
			return Collections.emptyMap();
		List<File> files = mongoTemplate.find(
				new Query(where("path").regex(
						"^" + path.replaceAll("\\.", "\\\\.") + "/[^/]*$")),
				File.class);
		for (File f : files) {
			String name = f.getPath();
			map.put(name.substring(name.lastIndexOf('/') + 1), !f.isDirectory());
		}
		Map<String, Boolean> sortedMap = new TreeMap<String, Boolean>(
				new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						Boolean b1 = map.get(o1);
						Boolean b2 = map.get(o2);
						if (b2 == null)
							return -1;
						if (b1 == null)
							return 1;
						int i = b2.compareTo(b1);
						return i != 0 ? i : o1.compareTo(o2);
					}
				});
		sortedMap.putAll(map);
		return sortedMap;
	}

	private static class File implements Serializable {

		private static final long serialVersionUID = -7690537474523537861L;

		@Id
		private String path;

		private boolean directory;

		private long lastModified;

		private byte[] data;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public boolean isDirectory() {
			return directory;
		}

		public void setDirectory(boolean directory) {
			this.directory = directory;
		}

		public long getLastModified() {
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			this.lastModified = lastModified;
		}

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}

	}

}