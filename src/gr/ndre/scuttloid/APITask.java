package gr.ndre.scuttloid;

import java.io.InputStream;

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
    }
	
	Callback callback;
	String url;
	String username;
	String password;
	DefaultHandler handler;
	
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
		} catch (Exception e) {
			// TODO Properly display error messages
			System.out.println(e.getMessage());
		}
		client.close();
		return null;
	}
	
    public void onPostExecute(Void param)
    {
        this.callback.onDataReceived(this.handler);
    }

}
