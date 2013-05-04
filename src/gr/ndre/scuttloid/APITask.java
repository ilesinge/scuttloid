/**
 * Scuttloid - Semantic Scuttle Android Client
 * Copyright (C) 2013 Alexandre Gravel-Raymond
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gr.ndre.scuttloid;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Xml;

public class APITask extends AsyncTask<Void, Void, Void> {

	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	
	public static final int GENERIC_ERROR = 1000;
	public static final int UNKNOWN_HOST = 1001;
	public static final int PARSE_ERROR = 1002;
	public static final int SSL_ERROR = 1003;
	public static final int TIMEOUT_ERROR = 1004;
	
	public static final int CONNECTION_TIMEOUT = 5000;
	public static final int SOCKET_TIMEOUT = 15000;
	
	protected Callback callback;
	protected String url;
	protected String username;
	protected String password;
	protected DefaultHandler handler;
	protected int status;
	protected int method = METHOD_GET;
	protected List<NameValuePair> data;
	protected ArrayList<Integer> acceptable_statuses = new ArrayList<Integer>();
	protected boolean accept_all_certs = false;
	
	APITask(Callback task_callback, String pref_username, String pref_password) {
		this.callback = task_callback;
		this.username = pref_username;
		this.password = pref_password;
	}
	
	public void setURL(String api_url) {
		this.url = api_url;
	}
	
	public void setHandler(DefaultHandler xml_handler) {
		this.handler = xml_handler;
	}

	public void setData(List<NameValuePair> data_list) {
		this.data = data_list;
	}
	
	public void setMethod(int method_id) {
		this.method = method_id;
	}
	
	public void addAcceptableStatus(int status_id) {
		this.acceptable_statuses.add(status_id);
	}
	
	public void acceptAllCerts(boolean set_accept_all_certs) {
		this.accept_all_certs = set_accept_all_certs;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		DefaultHttpClient client = getClient();
		HttpRequestBase request = buildRequest();
		if (request != null) {
			executeRequest(client, request);
		}
		return null;
	}

	protected DefaultHttpClient getClient() {
		DefaultHttpClient client;
		if (this.url.startsWith("https://") & this.accept_all_certs) {
			try {
				SchemeRegistry schemeRegistry = new SchemeRegistry();
				schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
				schemeRegistry.register(new Scheme("https", new TrustingSSLSocketFactory(), 443));
				HttpParams params = new BasicHttpParams();
				ClientConnectionManager connectionManager = 
					    new ThreadSafeClientConnManager(params, schemeRegistry);
				client = new DefaultHttpClient(connectionManager, params);
			} catch (Exception e) {
				client = new DefaultHttpClient();
			}
		}
		else {
			client = new DefaultHttpClient();
		}
		return client;
	}

	protected void executeRequest(HttpClient client, HttpRequestBase request) {
		try {
			HttpResponse response = client.execute(request);
			this.status = response.getStatusLine().getStatusCode();
			if (!this.isError(this.status)) {
				this.parseResponse(response);
			}
		}
		catch (UnknownHostException e) {
			this.status = UNKNOWN_HOST;
		}
		catch (SAXException e) {
			this.status = PARSE_ERROR;
		}
		catch (SSLHandshakeException e) {
			this.status = SSL_ERROR;
		}
		catch (SSLPeerUnverifiedException e) {
			this.status = SSL_ERROR;
		}
		catch (ConnectTimeoutException e) {
			this.status = TIMEOUT_ERROR;
		}
		catch (SocketTimeoutException e) {
			this.status = TIMEOUT_ERROR;
		}
		catch (Exception e) {
			this.status = GENERIC_ERROR;
			System.out.println(e.getClass().getName());
		}
	}
	
	protected boolean isError(int status_id) {
		return status_id >= 300 & !this.acceptable_statuses.contains(status_id);
	}

	protected HttpRequestBase buildRequest() {
		HttpRequestBase request;
		if (this.method == METHOD_POST) {
			request = new HttpPost(this.url);
			if (this.data != null) {
				try {
					UrlEncodedFormEntity entity = new UrlEncodedFormEntity(this.data, HTTP.UTF_8);
					((HttpEntityEnclosingRequestBase) request).setEntity(entity);
				}
				catch (UnsupportedEncodingException e) {
					this.status = GENERIC_ERROR;
					return null;
				}
			}
		}
		else {
			request = new HttpGet(this.url);
		}
		
		// Set timeout limits
		BasicHttpParams http_parameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(http_parameters, CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(http_parameters, SOCKET_TIMEOUT);
		request.setParams(http_parameters);
		
		// Add Basic Authentication header
		this.addAuthHeader(request);
		
		return request;
	}

	protected void parseResponse(HttpResponse response) throws IOException, SAXException {
		if (this.handler != null) {
			InputStream content = response.getEntity().getContent();
			Xml.parse(content, Xml.Encoding.UTF_8, this.handler);
		}
	}
	
	@Override
	public void onPostExecute(Void param) {
		if (this.isError(this.status)) {
			this.callback.onError(this.status);
		}
		else {
			this.callback.onDataReceived(this.handler, this.status);
		}
	}
	
	protected void addAuthHeader(HttpRequestBase request) {
		String authentication = this.username + ":" + this.password;
		String encodedAuthentication = Base64.encodeToString(authentication.getBytes(), Base64.NO_WRAP);
		request.addHeader("Authorization", "Basic " + encodedAuthentication);
	}

	public interface Callback {
		void onDataReceived(DefaultHandler handler, int status);
		void onError(int status);
	}
	
}
