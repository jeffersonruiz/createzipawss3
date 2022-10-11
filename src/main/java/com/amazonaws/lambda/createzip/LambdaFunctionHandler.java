package com.amazonaws.lambda.createzip;

import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {

	private AWSCredentials credentials = new BasicAWSCredentials("",
			"");

	private AmazonS3 s3client = AmazonS3ClientBuilder.standard()
			.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(Regions.US_EAST_1).build();

	Base64 base64 = new Base64();

	private String bucketName = "pruebaslambdas";
	private String files[] = { "Archivos/ArchivoI.jpg", "Archivos/archivoII.jpg", "Ubicacion/archivoIII", "UbicacionII/ArchivoIV.txt" };

	@Override
	public String handleRequest(Object input, Context context) {
		// context.getLogger().log("Input: " + input);

		File zip = zipUploadsToNewTemp(context);

		context.getLogger().log("zip: " + zip);

		String fileBase64 = encodeBase64(context, zip);

		context.getLogger().log("fileBase64: " + fileBase64);


		// TODO: implement your handler
		return "Ok";
	}

	public File zipUploadsToNewTemp(Context context) {
		byte[] buffer = new byte[1024];
		File tempZipFile = null;

		try {
			tempZipFile = File.createTempFile(UUID.randomUUID().toString(), ".zip");
		} catch (Exception e) {
			context.getLogger().log("Could not create Zip file\": " + e);
		}

		try (FileOutputStream fileOutputStream = new FileOutputStream(tempZipFile);
				ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

			for (String file : files) {
				S3ObjectInputStream inputStream = getStreamFromS3(file, context);
				ZipEntry zipEntry = new ZipEntry(file.split("/")[1]);
				zipOutputStream.putNextEntry(zipEntry);
				writeStreamToZip(buffer, zipOutputStream, inputStream, context);
				inputStream.close();
			}
			zipOutputStream.closeEntry();
			zipOutputStream.close();
			return tempZipFile;
		} catch (Exception e) {

		}

		return tempZipFile;
	}

	private S3ObjectInputStream getStreamFromS3(String file, Context context) {
		try {
			S3ObjectInputStream inputStream = s3client.getObject(bucketName, file).getObjectContent();
			return inputStream;
		} catch (Exception e) {
			context.getLogger().log("Unkown Error communicating with S3 for file: " + e);
		}
		return null;
	}

	private void writeStreamToZip(byte[] buffer, ZipOutputStream zipOutputStream, S3ObjectInputStream inputStream,
			Context context) {
		try {
			int len;
			while ((len = inputStream.read(buffer)) > 0) {
				zipOutputStream.write(buffer, 0, len);
			}
		} catch (Exception e) {
			context.getLogger().log("Could not write stream to zip: " + e);
		}
	}

	private String encodeBase64(Context context, File file) {
		byte[] fileArray = new byte[(int) file.length()];
		InputStream inputStream;

		String encodedFile = "";
		try {
			inputStream = new FileInputStream(file);
			inputStream.read(fileArray);
			encodedFile = base64.encodeToString(fileArray);
		} catch (Exception e) {
			context.getLogger().log("Could not create encode base64 " + e);
		}
		return encodedFile;
	}

}
