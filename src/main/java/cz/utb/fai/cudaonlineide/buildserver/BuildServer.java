package cz.utb.fai.cudaonlineide.buildserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * BuildServer class provides methods for work with build server.
 * 
 * @author Belanec
 *
 */
public class BuildServer {
	
	/**
	 * Method uploads ZIP file with project to build server. If it complete, build server build project.
	 * 
	 * @param zipFile ZIP file to upload.
	 * @return UUID of build process.
	 * @throws ClientProtocolException Throw if HTTP post request failed.
	 * @throws IOException Throw if HTTP connection is not established.
	 */
	public String uploadAndBuild(File zipFile) throws ClientProtocolException, IOException {
		
		if(zipFile == null) {
			Logger.getLogger(BuildServer.class.getName()).log(Level.SEVERE, null, "ERROR: File is null.");
			return null;
		}
		
		Path zipPath = Paths.get(zipFile.getPath());
		
		try {
			byte[] zipData = Files.readAllBytes(zipPath);
			
			if(Byte.compare(zipData[0], (byte) 0x50) != 0 || Byte.compare(zipData[1], (byte) 0x4B) != 0) {
				Logger.getLogger(BuildServer.class.getName()).log(Level.SEVERE, null, "ERROR: Invalid file type.");
				return null;
			}
		} catch (IOException e) {
			Logger.getLogger(BuildServer.class.getName()).log(Level.SEVERE, null, e);
		}
		
		if(zipFile.exists() && zipFile.isFile() && zipFile.canRead()) {
		
			String completeUrl = BuildServerConstants.URL + BuildServerConstants.UPLOAD;
			String output = "";
			
	        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
	        	
	            HttpPost httppost = new HttpPost(completeUrl);
	            httppost.addHeader(BuildServerConstants.CONTENT_TYPE, ContentType.APPLICATION_OCTET_STREAM.toString());
	
	            HttpEntity reqEntity = new FileEntity(zipFile);
	            httppost.setEntity(reqEntity);            
	
	            System.out.println("BuildServer LOG [Executing upload and build request: " + httppost.getRequestLine() + "]");
	
	            try ( CloseableHttpResponse response = httpclient.execute(httppost)) {
	            	
	            	System.out.println("BuildServer LOG [Returned response: " + response.getStatusLine() + "]");
	                
	                HttpEntity resEntity = response.getEntity();
	                if (resEntity != null) {
	                	output = EntityUtils.toString(response.getEntity());
	                }
	                EntityUtils.consume(resEntity);
	            }
	        }
			
	
			if(output == null || output.isEmpty()) {
				return null;
			}
			
			Gson gson = new Gson();
			JsonObject json = gson.fromJson(output, JsonObject.class);
	
			return json.get(BuildServerConstants.UUID).getAsString();

		} else {
			Logger.getLogger(BuildServer.class.getName()).log(Level.SEVERE, null, "ERROR: Zip cannot be upload.");
		}
		
		return null;
	}
	
	/**
	 * Method provides execution of build project.
	 * 
	 * @param uuid UUID of build process.
	 * @param arguments Arguments for project build.
	 * @throws ClientProtocolException Throw if HTTP post request failed.
	 * @throws IOException Throw if HTTP connection is not established.
	 */
	public void execute(String uuid, String arguments) throws ClientProtocolException, IOException {
		
		if(uuid != null && !uuid.isEmpty()) {
		
			String completeUrl = BuildServerConstants.URL + "/" + uuid + BuildServerConstants.EXECUTE;
			
			try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
	        	
	            HttpPost httppost = new HttpPost(completeUrl);
	            httppost.addHeader(BuildServerConstants.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
	            
	            HttpEntity reqEntity;
	            
	            if(arguments == null || arguments.isEmpty()) {
	            	reqEntity = new StringEntity("{}");
	            } else {
	            	reqEntity = new StringEntity("{\"" + BuildServerConstants.ARGUMENTS + "\":\"" + arguments + "\"}");
	            }
	
	            httppost.setEntity(reqEntity);  
	            
	            System.out.println("BuildServer LOG [Executing execute request: " + httppost.getRequestLine() + "]");
	
	            try ( CloseableHttpResponse response = httpclient.execute(httppost)) {
	            	
	            	System.out.println("BuildServer LOG [Returned response: " + response.getStatusLine() + "]");
	                
	                HttpEntity resEntity = response.getEntity();
	                EntityUtils.consume(resEntity);
	            }
	        }	
		} else {
			Logger.getLogger(BuildServer.class.getName()).log(Level.SEVERE, null, "ERROR: Invalid UUID of process.");
		}
	}
	
	/**
	 * Method returns result of process build or process execution.
	 * 
	 * @param uuid UUID of build process.
	 * @return Text result of build process.
	 * @throws ClientProtocolException Throw if HTTP post request failed.
	 * @throws IOException Throw if HTTP connection is not established.
	 */
	public String[] getResult(String uuid, String outputType) throws ClientProtocolException, IOException {
		
		if(uuid != null && !uuid.isEmpty() && (outputType.equals(BuildServerConstants.COMPILATION_OUTPUT) || outputType.equals(BuildServerConstants.EXECUTION_OUTPUT))) {
		
			String completeUrl = BuildServerConstants.URL + "/" + uuid;
			String output = "";
			
			try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
	        	
	            HttpGet httpget = new HttpGet(completeUrl);
	            
	            System.out.println("BuildServer LOG [Executing get result request: " + httpget.getRequestLine() + "]");
	
	            try ( CloseableHttpResponse response = httpclient.execute(httpget)) {
	            	
	            	System.out.println("BuildServer LOG [Returned response: " + response.getStatusLine() + "]");
	                
	                HttpEntity resEntity = response.getEntity();
	                if (resEntity != null) {
	                    output = EntityUtils.toString(response.getEntity());
	                }
	                EntityUtils.consume(resEntity);
	            }
	        }		
			
			if(output == null || output.isEmpty()) {
				return null;
			}
			
			Gson gson = new Gson();
			JsonObject json = gson.fromJson(output, JsonObject.class);
			
			if(json == null || json.get(outputType) == null) {
				return null;
			}
			
			String compilationFailed = null;
			
			if(outputType.equals(BuildServerConstants.COMPILATION_OUTPUT)) {
				if(json.get(BuildServerConstants.COMPILATION_FAILED) != null) {
					compilationFailed = json.get(BuildServerConstants.COMPILATION_FAILED).getAsString();
				}
			}
			
			if(compilationFailed == null) {
				String[] outputArray = {json.get(outputType).getAsString()};
				return outputArray;
			}
			
			String[] outputArray = {json.get(outputType).getAsString(), compilationFailed};
			return outputArray;
		
		} else {
			Logger.getLogger(BuildServer.class.getName()).log(Level.SEVERE, null, "ERROR: Invalid process.");
		}
		
		return null;
	}
}
