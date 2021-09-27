package com.maviucak.anttasks;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Echo;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class S3UploaderTask extends Task
{
	private ArrayList<FileSet> mFileSets = new ArrayList<FileSet>();
	private boolean mVerbose = false;
	private String accessKey;
	private String secretKey;
	private String region;
	private String bucket;
	private String basePath = "";
	private String propertyName;

	//Variables initialized internally
	private AmazonS3 s3;

	public void add(FileSet fSet)
	{
		mFileSets.add(fSet);
	}

	public void setVerbose(boolean verbose)
	{
		mVerbose = verbose;
	}

	public void setAccessKey(String accessKey)
	{
		this.accessKey = accessKey;
	}

	public void setSecretKey(String secretKey)
	{
		this.secretKey = secretKey;
	}

	public void setRegion(String region)
	{
		this.region = region;
	}

	public void setBucket(String bucket)
	{
		this.bucket = bucket;
	}

	public void setBasePath(String basePath)
	{
		if (basePath == null)
			basePath = "";
		else
			this.basePath = basePath;
	}
	
	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}

	public void execute() throws BuildException
	{
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
		s3 = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withRegion(region).build();

		if (basePath == null)
			basePath = "";

		if (mFileSets.size() == 0)
			throw new BuildException("At least one fileset should be specified");

		log("Uploading files...");
		ArrayList<File> fileList = new ArrayList<File>();
		for (int i = 0; i < mFileSets.size(); i++)
		{
			DirectoryScanner directoryScanner = mFileSets.get(i).getDirectoryScanner(getProject());
			String[] includedFiles = directoryScanner.getIncludedFiles();

			for (int j = 0; j < includedFiles.length; j++)
			{
				File f = new File(mFileSets.get(i).getDir().getAbsolutePath() + File.separator + includedFiles[j]);
				fileList.add(f);
			}
		}

		for (File file : fileList)
		{
			try
			{
				Path path = Paths.get(file.getPath());
				byte[] bytes = Files.readAllBytes(path);
				ObjectMetadata metaData = new ObjectMetadata();
				if (bytes != null)
					metaData.setContentLength(bytes.length);
				s3.putObject(bucket, basePath + file.getName(), new ByteArrayInputStream(bytes), metaData);
				String url = s3.getUrl(bucket, basePath + file.getName()).toString();
				if (mVerbose)
				{
					Echo echo = (Echo)getProject().createTask("echo");
					echo.setMessage("Uploaded to " + url);
					echo.execute();
				}
				if (propertyName != null && !propertyName.isEmpty())
				{
					Property property = (Property)getProject().createTask("property");
					property.setName(propertyName);
					property.setValue(url);
					property.execute();
				}
			}
			catch (IOException e)
			{
				throw new BuildException(e);
			}
		}

	}
}
