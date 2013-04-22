package com.example.avangard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.app.IntentService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/**
 * @author dmitry@inflim.com
 * 
 */
public class CustomService extends IntentService {

	private static ExecutorService		executor	= Executors.newFixedThreadPool(1);

	public static final String			MESSENGER	= "MESSENGER";
	public static final String			URL_PARAM	= "URL";

	public static final int				OK			= 0x001;
	public static final int				FAILED		= 0x002;
	public static final int				CANCELED	= 0x003;

	private static final String			TAG			= "Avangard";
	protected static final String		URL_DATA	= "DATA";

	private static ConcurrentSkipListMap<Integer, Future<DownloadTask>>	futures =
			new ConcurrentSkipListMap<Integer, Future<DownloadTask>>();

	public CustomService() {
		super("CustomService");
	}

	public class DownloadTask implements Runnable {

		private Intent	intent;
		private String	url;

		public DownloadTask(Intent intent) {
			this.intent = intent;
			url = intent.getStringExtra(URL_PARAM);
		}

		// use url hashcode for map
		@Override
		public int hashCode() {
			return url.hashCode();
		}
		@Override
		public void run() {
			try {
				String data = null;
				Messenger messenger = (Messenger) intent.getExtras().get(MESSENGER);
				Message msg = Message.obtain();
				msg.obj = url;

				try {
					data = queryUrl(url);
				} catch (IOException e) {
					Log.e(TAG, "Exception in queryUrl", e);
					msg.what = FAILED;
				}
				if (data == null) {
					msg.what = CANCELED;
				} else {
					msg.what = OK;

					msg.getData().putString(URL_DATA, data);
				}
				try {
					messenger.send(msg);
				} catch (android.os.RemoteException e) {
					Log.w(TAG, "Exception sending message back to activity", e);
				}
			} catch (Exception e) {
				Log.e(TAG, "Exception in DownloadTask", e);
			} finally {
				futures.remove(this.hashCode());
			}
		}



		private CustomService getOuterType() {
			return CustomService.this;
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// remove all tasks, if queue is not supported
		if (intent.hasExtra("cancelAll") && !futures.isEmpty()) {
			for (Integer key : futures.keySet()) {
				futures.get(key).cancel(true);
			}
		}

		// if for some reason task has completed but didn't remove itself
		// also, remove cancelled tasks
		for (Integer key : futures.keySet()) {
			if (futures.get(key).isDone())
				futures.remove(key);
		}

		DownloadTask task = new DownloadTask(intent);
		Future<DownloadTask> future = executor.submit(task, task);
		futures.put(task.hashCode(), future);
	}

	private String queryUrl(String url) throws IOException {
		BufferedReader reader = null;
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) new URL(url).openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setReadTimeout(10 * 1000);
			urlConnection.connect();

			reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			StringBuilder sb = new StringBuilder();

			// use small buffer to periodically control if thread is interrupted
			char[] buf = new char[128];
			while ((reader.read(buf)) != -1) {
				if (Thread.currentThread().isInterrupted())
					return null;

				sb.append(buf);
			}
			return sb.toString();
		} catch (IOException e) {
			throw e;
		} finally {
			if (reader != null)
				reader.close();
			if (urlConnection != null) {
				urlConnection.getInputStream().close();
				urlConnection.disconnect();
			}
		}

	}
}
