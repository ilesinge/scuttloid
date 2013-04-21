package gr.ndre.scuttloid;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HTTP;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Xml;

public class APITask extends AsyncTask<Void, Void, Void> {

	public static interface Callback {
        public void onDataReceived(DefaultHandler handler, int status);
        public void onError(int status);
    }
	
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	
	protected Callback callback;
	protected String url;
	protected String username;
	protected String password;
	protected DefaultHandler handler;
	protected int status = 0;
	protected int method = METHOD_GET;
	protected List<NameValuePair> data;
	protected ArrayList<Integer> acceptable_statuses = new ArrayList<Integer>();
	
	public static final int GENERIC_ERROR = 1000;
	public static final int UNKNOWN_HOST = 1001;
	public static final int PARSE_ERROR = 1002;
	public static final int SSL_ERROR = 1003;
	
	APITask(Callback callback, String username, String password) {
		this.callback = callback;
		this.username = username;
		this.password = password;
	}
	
	public void setURL(String url) {
		this.url = url;
	}
	
	public void setHandler(DefaultHandler handler) {
		this.handler = handler;
	}

	public void setData(List<NameValuePair> data) {
		this.data = data;
	}
	
	public void setMethod(int method) {
		this.method = method;
	}
	
	public void addAcceptableStatus(int status) {
		this.acceptable_statuses.add(status);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		AndroidHttpClient client = AndroidHttpClient.newInstance("gr.ndre.scuttloid");
		HttpRequestBase request = buildRequest();
		if (request != null) {
			executeRequest(client, request);
		}
		client.close();
		return null;
	}

	protected void executeRequest(AndroidHttpClient client, HttpRequestBase request) {
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
		catch (Exception e) {
			this.status = GENERIC_ERROR;
			//System.out.println(e.getClass().getName());
		}
	}
	
	protected boolean isError(int status) {
		return (status >= 300 & !acceptable_statuses.contains(status));
	}

	protected HttpRequestBase buildRequest() {
		HttpRequestBase request;
		if (this.method == METHOD_POST) {
			request = new HttpPost(this.url);
			if (this.data != null) {
				try {
					UrlEncodedFormEntity entity = new UrlEncodedFormEntity(this.data, HTTP.UTF_8);
					((HttpEntityEnclosingRequestBase) request).setEntity(entity);
				} catch (UnsupportedEncodingException e) {
					this.status = GENERIC_ERROR;
					return null;
				}
			}
		} else {
			request = new HttpGet(this.url);
		}
		
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
	
	public void onPostExecute(Void param)
	{
		if (this.isError(this.status)) {
			this.callback.onError(this.status);
		}
		else {
			this.callback.onDataReceived(this.handler, this.status);
		}
	}
	
	protected void addAuthHeader(HttpRequestBase request) {
		String authentication = username+":"+password;
		String encodedAuthentication = Base64.encodeToString(authentication.getBytes(), Base64.NO_WRAP);
		request.addHeader("Authorization", "Basic " + encodedAuthentication);
	}

}
