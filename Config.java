package com.hiyo.hcf.util;

import com.hiyo.hcf.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
	private static Logger logger = LoggerFactory.getLogger(Config.class);
	
	private static Config instance = null;
	private List<Properties> propertiesList = new ArrayList<Properties>();	
	private final String[] paths = {Constants.RESOURCE_PATH};
	
	private Config() {
		for (String path : paths) {
			String resourcePath = path; 
			
			InputStream stream = null;
		    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		    if (classLoader != null) {
		        stream = classLoader.getResourceAsStream(resourcePath);
		    }
		    if (stream == null) {
		        stream = Config.class.getResourceAsStream(resourcePath);
		    }
		    if (stream == null) {
		        stream = Config.class.getClassLoader().getResourceAsStream(resourcePath);
		    }
		    if (stream == null) {
		        logger.error(String.format("Failed to load the configuration file %s as resource", resourcePath));
		    }
			
		    Properties properties = new Properties();
			try {
				properties.load(stream);
			} catch (IOException e) {			
				logger.error(String.format("Failed to load the configuration file %s, msg %s", resourcePath, e.getMessage()));
			} finally {
				try {
					stream.close();
				} catch (IOException e) {}
			}
			
			this.propertiesList.add(properties);
		}
		
	}
	
	public static InputStream getScriptStream(String path) {
		String resourcePath = path; 
		
		InputStream stream = null;
	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    if (classLoader != null) {
	        stream = classLoader.getResourceAsStream(resourcePath);
	    }
	    if (stream == null) {
	        stream = Config.class.getResourceAsStream(resourcePath);
	    }
	    if (stream == null) {
	        stream = Config.class.getClassLoader().getResourceAsStream(resourcePath);
	    }
	    if (stream == null) {
	        logger.error(String.format("Failed to load the configuration file %s as resource", resourcePath));
	    }
	    
	    return stream;
	}
	
	public synchronized static Config getConfig(){
		if (instance == null) {
			instance = new Config();
		}
		return instance;
	}
	
	public String get(String key) {
		String returnValue = null;
		for (Properties p : this.propertiesList) {
			returnValue = p.getProperty(key);
			if (returnValue != null) {
				Matcher matcher = PATTERN.matcher(returnValue);
				StringBuffer buffer = new StringBuffer();
				//变量替换
				while (matcher.find()) {
					String matcherKey = matcher.group(1);
					String matchervalue = p.getProperty(matcherKey);
					if (matchervalue != null) {
						matcher.appendReplacement(buffer, matchervalue);
					}
					matcher.appendTail(buffer);
					return buffer.toString();
				}
				return returnValue;
			}
		}
		return returnValue;
	}


	private static final Pattern PATTERN = Pattern
			.compile("\\$\\{([^\\}]+)\\}");

	
}
