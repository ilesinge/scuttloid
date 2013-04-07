package gr.ndre.scuttloid;

import java.io.InputStream;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Xml;

public class APITask extends AsyncTask<Void, Void, Void> {

	public static interface Callback {
        public void onDataReceived(DefaultHandler handler);
        public void onError(int status);
    }
	
	protected Callback callback;
	protected String url;
	protected String username;
	protected String password;
	protected DefaultHandler handler;
	protected int status = 0;
	
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

	@Override
	protected Void doInBackground(Void... params) {
		AndroidHttpClient client = AndroidHttpClient.newInstance("gr.ndre.scuttloid");
		HttpGet request = new HttpGet(this.url);
		
		// Add Basic Authentication header
		String authentication = username+":"+password;
		String encodedAuthentication = Base64.encodeToString(authentication.getBytes(), Base64.NO_WRAP);
		request.addHeader("Authorization", "Basic " + encodedAuthentication);
		
		try {
			HttpResponse response = client.execute(request);
			int status = response.getStatusLine().getStatusCode();
			if (status >= 300) {
				this.status = status;
				// TODO : parse eventual error message !
				client.close();
				return null;
			}
			InputStream content = response.getEntity().getContent();
			Xml.parse(content, Xml.Encoding.UTF_8, this.handler);
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
		client.close();
		return null;
	}
	
	public void onPostExecute(Void param)
	{
		if (this.status > 0) {
			this.callback.onError(this.status);
		}
		else {
			this.callback.onDataReceived(this.handler);
		}
	}

}
