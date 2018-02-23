package com.qmetry;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import hudson.model.BuildListener;

public class QMetryConnection {

    private String url;
    private String key;

    public QMetryConnection(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public String getUrl() {
        return this.url;
    }

    public String getKey() {
        return this.key;
    }

    public boolean validateConnection() {
        // TODO validate URL,Key
        return true;
    }

    public void uploadFileToTestSuite(String filePath, String testSuiteName, String automationFramework,
            String buildName, String platformName, String pluginName, BuildListener listener)
            throws QMetryException {
		try
		{
			CloseableHttpClient httpClient = null;
			CloseableHttpResponse response = null;
			
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addTextBody("entityType", automationFramework, ContentType.TEXT_PLAIN);
			if(testSuiteName!=null && !testSuiteName.isEmpty())
				builder.addTextBody("testsuiteId", testSuiteName, ContentType.TEXT_PLAIN);
			if(buildName!=null && !buildName.isEmpty())
				builder.addTextBody("buildID", buildName, ContentType.TEXT_PLAIN);
			if(platformName!=null && !platformName.isEmpty())
				builder.addTextBody("platformID", platformName, ContentType.TEXT_PLAIN);

			File f = new File(filePath);
			builder.addPart("file", new FileBody(f));
			HttpEntity multipart = builder.build();

			HttpPost uploadFile = new HttpPost(getUrl() + "/rest/import/createandscheduletestresults/1");
			uploadFile.addHeader("accept", "application/json");
			uploadFile.addHeader("scope", "default");
			uploadFile.addHeader("apiKey", getKey());
			uploadFile.setEntity(multipart);

			httpClient = HttpClients.createDefault();
			response = httpClient.execute(uploadFile);
			String responseString = EntityUtils.toString(response.getEntity());
			if (response.getStatusLine().getStatusCode() == 200) 
			{
				try
				{
					JSONObject jsonresponse = (JSONObject) new JSONParser().parse(responseString);
					if(jsonresponse.get("success").toString().equals("true"))
					{
						JSONArray data = (JSONArray) jsonresponse.get("data");
						JSONObject dataObj = (JSONObject) data.get(0);
						listener.getLogger().println(pluginName + " : Test Suite ID : "+dataObj.get("testsuiteId").toString());
						listener.getLogger().println(pluginName + " : Build ID : "+dataObj.get("buildID").toString());
						listener.getLogger().println(pluginName + " : Platform ID : "+dataObj.get("platformID").toString());
					}
					else
					{
						listener.getLogger().println(pluginName + " : Server response : '"+responseString+"'");
						throw new QMetryException("Error uploading file to server!");
					}
				}
				catch(ParseException e)
				{
					listener.getLogger().println(pluginName + " : Server response : '"+responseString+"'");
					throw new QMetryException("Error uploading file to server!");
				}
			}
			else
			{
				listener.getLogger().println(pluginName + " : Server Response : '"+responseString+"'");
				throw new QMetryException("Error uploading file to server!");
			}
			httpClient.close();
			response.close();
		}
		catch(IOException e)
		{
			throw new QMetryException("Failed to upload result files to QMetry!");
		}
	}
}