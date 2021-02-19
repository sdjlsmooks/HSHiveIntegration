package com.solutions.health;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class HSHiveIntegration {

	public String getTickets() {
		String retVal = "";
		try {

			HostnameVerifier allHostsValid = installAllTrustingTrustManager();

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

			String webPage = "https://helpdesk/api/";
			String endPoint = "Tickets?count=100";
			String username = "SP\\davidl";
			String password = "3edcVFR$3edc";

			// Filter out only tickets as new as today
			SimpleDateFormat formatter = new SimpleDateFormat("YYYY-mm-dd");
			Date today = new Date();
			String dateFrom="?dateFrom="+formatter.format(today);
			
			// Create Basic authentication string
			String authString = username + ":" + password;
			byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
			String authStringEnc = new String(authEncBytes);

			// Perform query against the helpdesk URL as agove.
			URL url = new URL(webPage+endPoint+dateFrom);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
			InputStream is = urlConnection.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			
			// Read entire reponse string into memory
			int numCharsRead;
			char[] charArray = new char[1024];
			StringBuffer sb = new StringBuffer();
			while ((numCharsRead = isr.read(charArray)) > 0) {
				sb.append(charArray, 0, numCharsRead);
			}
			retVal = sb.toString();
			
			// Debugging
			System.out.println("*** BEGIN ***");
			System.out.println(retVal);
			System.out.println("*** END ***");
			
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return retVal;
	}

	private HostnameVerifier installAllTrustingTrustManager() throws NoSuchAlgorithmException, KeyManagementException {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		return allHostsValid;
	}

	public static void main(String[] args) {
		HSHiveIntegration hshi = new HSHiveIntegration();

		try {

			JSONParser parser = new JSONParser();

			// Data drive with command line argument here
			// Read in desired categories to look for. (configuration in JSON)
			String categoryStr = "";
			byte[] encoded = Files.readAllBytes(Paths.get("H:\\projects\\HiveIntegration\\HiveIntegration.json"));
			categoryStr = new String(encoded, StandardCharsets.UTF_8);
			JSONObject desiredCategories = (JSONObject) parser.parse(categoryStr);
			JSONArray categories = (JSONArray) desiredCategories.get("categories");
			
			// Set to contain desired categories (makes later processing easier)
			HashSet<Long> desiredCategorySet = new HashSet<Long>();
			Iterator<Long> categoryItr = categories.iterator();
			while (categoryItr.hasNext()) {
				desiredCategorySet.add((Long) categoryItr.next());
			}


			// Get tickets from server (this will move to a more
			// approriate place later)  Debugging parsing and hive integration
			// at the moment.
			String tickets = hshi.getTickets();
			JSONArray ticketArr = (JSONArray) parser.parse(tickets);
			System.out.println("Length: " + ticketArr.size());

			// Loop through all tickets retrieved
			// If (desiredCategories.contains(ticket.CategoryID))
			//     create project
			// else
			//     do not create project
			//
			Iterator<JSONObject> iterator = ticketArr.iterator();
			while (iterator.hasNext()) {
				JSONObject value = iterator.next();
				Object issueID = value.get("IssueID");
				System.out.println("IssueID     " + issueID);

				Object categoryName = value.get("Category");
				System.out.println("Category     " + categoryName);
				Object categoryID = value.get("CategoryID");
				System.out.println("Category ID: " + categoryID);
				
				if (desiredCategorySet.contains(categoryID)) {
					System.out.println("\tCreate project");
				} else {
					System.out.println("\tDO NOT create project");
				}
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
