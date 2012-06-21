package de.kp.ames.webdav;
/**
 *	Copyright 2012 Dr. Krusche & Partner PartG
 *
 *	AMES-WebDAV is free software: you can redistribute it and/or 
 *	modify it under the terms of the GNU General Public License 
 *	as published by the Free Software Foundation, either version 3 of 
 *	the License, or (at your option) any later version.
 *
 *	AMES-WebDAV is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * 
 *  See the GNU General Public License for more details. 
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this software. If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertySet;

import org.json.JSONArray;
import org.json.JSONObject;

public class WebDAVClient {

	private String uri;
	private HttpClient client;

	/**
	 * Constructor
	 * 
	 * @param alias
	 * @param keypass
	 * @param uri
	 */
	public WebDAVClient(String alias, String keypass, String uri) {

		/*
		 * Register request uri
		 */
		this.uri = uri;
		
		/*
		 * Setup HttpClient
		 */
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(uri);

		HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		
		int maxHostConnections = 200;
		params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
		
		connectionManager.setParams(params);
		
		client = new HttpClient(connectionManager);
		client.setHostConfiguration(hostConfig);
		
		Credentials creds = new UsernamePasswordCredentials(alias, keypass);
		client.getState().setCredentials(AuthScope.ANY, creds);

	}
	
	
	/**
	 * Determines all level 1 resources of a certain webdav resource, 
	 * referred by the given uri; this is a method for a folder-like 
	 * listing
	 * 
	 * @return
	 */
	public JSONArray getResources() {

		/*
		 * Sorter to sort WebDAV folder by name
		 */
		Map<String, JSONObject> collector = new TreeMap<String, JSONObject>(new Comparator<String>(){
			public int compare(String name1, String name2) {
				return name1.compareTo(name2);
			}
		});
		
		try {
			
			DavMethod method = new PropFindMethod(uri, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_1);
			client.executeMethod(method);
			
			MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();
	
			MultiStatusResponse[] responses = multiStatus.getResponses();
		    MultiStatusResponse response;
	
		    /* 
		     * Determine base uri from uri
		     */
		    String base_uri = null;
		    
		    int first_slash = uri.indexOf("/", 8); // after http://
		    if (uri.endsWith("/")) base_uri = uri.substring(first_slash);
		    
	        int status = 200; // http ok
	        
		    for (int i=0; i<responses.length; i++) {
	
		    	response = responses[i];
		    	String href = response.getHref();
		    		    	
		    	/* 
		    	 * Ignore the current directory
		    	 */
		    	if (href.equals(base_uri)) continue;
		    	
		    	String name = href.substring(base_uri.length());
				// remove the final / from the name for directories
				if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
				
				
				// ============= properties =================================
				
				DavPropertySet properties = response.getProperties(status);
				
				/* 
				 * creationdate
				 */
				String creationdate = null;
				
				DavProperty<?> creationDate = properties.get("creationdate");
				if ((creationDate != null) && (creationDate.getValue() != null) ) creationdate = creationDate.getValue().toString();
								
				/* 
				 * lastModifiedDate
				 */
				String lastmodified = null;
				
				DavProperty<?> lastModifiedDate = properties.get("getlastmodified");
				if ((lastModifiedDate != null) && (lastModifiedDate.getValue() != null) ) {
					lastmodified = lastModifiedDate.getValue().toString();
					
				} else {
					lastmodified = creationdate;
				}
	
				
				/* 
				 * contenttype
				 */
				String contenttype = "text/plain";
				
				DavProperty<?> contentType = properties.get("getcontenttype");
				if ((contentType != null) && (contentType.getValue() != null) ) contenttype = contentType.getValue().toString();
				
				
				/* 
				 * getcontentlength
				 */
				String contentlength = "0";
				
				DavProperty<?> contentLength = properties.get("getcontentlength");
				if ((contentLength != null) && (contentLength.getValue() != null)) contentlength = contentLength.getValue().toString();
	
				/* 
				 * resource type
				 */
				String resourcetype = null;
				
				DavProperty<?> resourceType = properties.get("resourcetype");			
				if ((resourceType != null) && (resourceType.getValue() != null)) resourcetype = resourceType.getValue().toString();
	
				// title
				//DavProperty title = properties.get("title");
								
				// supportedlock
				//DavProperty supportedLock = properties.get("supportedlock");
	
				// displayname
				//DavProperty displayName = properties.get("displayname");
				
				/* 
				 * distinguish folder & file resource
				 */
				boolean isfolder = false;
				if ((resourcetype != null) && (resourcetype.indexOf("collection") != -1)) isfolder = true;
				
				/* 
				 * determine absolute url
				 */
				String resource_uri = uri + name;
				if (isfolder == true) resource_uri = resource_uri + "/";

				// this is a tribute to plone webdav server
				// we ignore file-like resources with content length = 0
				if ( (isfolder==false) && (contentlength == "0" )) continue;
				
				/*
				 * Convert to JSON object
				 */
				JSONObject jResource = new JSONObject();

				jResource.put("name", name);				
				jResource.put("uri",  resource_uri);

				jResource.put("creationDate", creationdate);
				jResource.put("lastModified", lastmodified);
				
				jResource.put("isfolder", !isfolder);
				
				if (isfolder == false) {	
					
					jResource.put("contentLength", contentlength);
					jResource.put("contentType",   contenttype);
				}
				
				collector.put(name, jResource);

		    }
						
			return new JSONArray(collector.values());

		} catch (Exception e)  {
	    	e.printStackTrace();
	    	
	    } finally {}

		
	    return new JSONArray();

	}
	
	/**
	 * A method to retrieve a File-based WebDAV resource
	 * 
	 * @param name
	 * @param mimetype
	 * @return
	 */
	public WebDAVFile getFile() {
		
		WebDAVFile file = null;
		
		try {

			GetMethod method = new GetMethod(uri);
			client.executeMethod(method);

			if (method.getStatusCode() == 200) {
								
				InputStream stream = method.getResponseBodyAsStream();
				file = new WebDAVFile(stream);
				
			}
			
		} catch (HttpException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {}
		
		return file;
		
	}
	
}
