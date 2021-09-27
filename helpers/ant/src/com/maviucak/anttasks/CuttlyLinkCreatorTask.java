package com.maviucak.anttasks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CuttlyLinkCreatorTask extends Task
{
	private String apiKey;
	private String urlText;
	private String shortLink;
	private String propertyName;

	public void setApiKey(String apiKey)
	{
		this.apiKey = apiKey;
	}

	public void setUrlText(String urlText)
	{
		this.urlText = urlText;
	}

	public void setShortLink(String shortLink)
	{
		this.shortLink = shortLink;
	}

	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}

	public void execute() throws BuildException
	{

		HttpURLConnection conn = null;
		try
		{
			String encodedURL = URLEncoder.encode(urlText, StandardCharsets.UTF_8.toString());
			URL url = new URL(
					"https://cutt.ly/api/api.php?key=" + apiKey + "&short=" + encodedURL + "&name=" + shortLink);
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200)
			{
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}

			String result = IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
			JsonObject jRoot = new JsonParser().parse(result).getAsJsonObject();
			int statusCode = jRoot.getAsJsonObject("url").getAsJsonPrimitive("status").getAsInt();
			switch (statusCode)
			{
			case 1:
				throw new IllegalStateException(
						"the shortened link comes from the domain that shortens the link, i.e. the link has already been shortened");
			case 2:
				throw new IllegalStateException("the entered link is not a link");
			case 3:
				throw new IllegalStateException("the preferred link name is already taken '" + shortLink + "'");
			case 4:
				throw new IllegalStateException("Invalid API key");
			case 5:
				throw new IllegalStateException("the link has not passed the validation. Includes invalid characters");
			case 6:
				throw new IllegalStateException("The link provided is from a blocked domain");
			case 7:
			{
				String retval = jRoot.getAsJsonObject("url").getAsJsonPrimitive("shortLink").getAsString(); //OK - the link has been shortened
				if (propertyName != null && !propertyName.isEmpty())
				{
					Property property = (Property)getProject().createTask("property");
					property.setName(propertyName);
					property.setValue(retval);
					property.execute();
				}
				break;
			}

			default:
				throw new IllegalStateException(
						"Unexpected status code " + statusCode + " please check cuttly api documentation");
			}
		}
		catch (IOException e)
		{
			throw new BuildException(e);
		}
		finally
		{
			if (conn != null)
				conn.disconnect();
		}
	}
}
