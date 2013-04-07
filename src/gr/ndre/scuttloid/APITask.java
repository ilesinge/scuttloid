package gr.ndre.scuttloid;

import java.io.InputStream;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
	protected int status;
	
	public static final int UNKNOWN_HOST = 1001;
	
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
			InputStream content = response.getEntity().getContent();
			Xml.parse(content, Xml.Encoding.UTF_8, this.handler);
		}
		catch (UnknownHostException e) {
			this.status = UNKNOWN_HOST;
		}
		catch (Exception e) {
			// TODO Properly display error messages
			System.out.println(e.getClass().getName());
		}
		client.close();
		return null;
	}
	
	public void onPostExecute(Void param)
	{
		if (this.status >= 400) {
			this.callback.onError(this.status);
		}
		else {
			this.callback.onDataReceived(this.handler);
		}
	}

}
