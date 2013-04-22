package com.example.avangard;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String ATM_URL = "https://dl.dropboxusercontent.com/u/11393460/atm.json";
	private static final String BANK_URL = 	"https://dl.dropboxusercontent.com/u/11393460/bank.json";
	private static final String DISCOUNTS_URL = "https://dl.dropboxusercontent.com/u/11393460/discounts.json";
	private static final String OFFICE_URL = "https://dl.dropboxusercontent.com/u/11393460/office.json";
	private static final String REC_URL = "https://dl.dropboxusercontent.com/u/11393460/rec.json";
	protected static final String	TAG	= "avangard";
	
	private boolean				supportQueue;
	
	private Map<Integer, TextView> textViewsMap = new HashMap<Integer, TextView>();

	private static Handler				handler;

	private static Messenger			messenger;
		  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		supportQueue = true;
		
		handler = new Handler() {
			public void handleMessage(Message message) {
				String url = (String) message.obj;
				switch(message.what){
					case CustomService.OK:
						Log.d(TAG, "Download ok: " + url);
						String urlData = message.getData().getString(CustomService.URL_DATA);
						
						textViewsMap.get(url.hashCode()).setText(urlData.substring(0, 150));
						break;
					case CustomService.FAILED:
						showToast("Download failed: " + url);
						Log.d(TAG, "Download failed: " + url);
						break;
					case CustomService.CANCELED:
						Log.d(TAG, "Download canceled: " + url);
						break;
					default:
						super.handleMessage(message);
						break;
				}

			};
		};
		messenger = new Messenger(handler);
		
		textViewsMap.clear();
		
		textViewsMap.put(ATM_URL.hashCode(), (TextView)findViewById(R.id.atmText));
		textViewsMap.put(BANK_URL.hashCode(), (TextView)findViewById(R.id.bankText));
		textViewsMap.put(DISCOUNTS_URL.hashCode(), (TextView)findViewById(R.id.discountsText));
		textViewsMap.put(OFFICE_URL.hashCode(), (TextView)findViewById(R.id.officeText));
		textViewsMap.put(REC_URL.hashCode(), (TextView)findViewById(R.id.recText));
		
		
		((Button) findViewById(R.id.atm_button)).setOnClickListener(new ButtonListener(ATM_URL));
		((Button) findViewById(R.id.bank_button)).setOnClickListener(new ButtonListener(BANK_URL));
		((Button) findViewById(R.id.discounts_button)).setOnClickListener(new ButtonListener(DISCOUNTS_URL));
		((Button) findViewById(R.id.office_button)).setOnClickListener(new ButtonListener(OFFICE_URL));
		((Button) findViewById(R.id.rec_button)).setOnClickListener(new ButtonListener(REC_URL));
		
		((CheckBox) findViewById(R.id.queue_support)).setOnCheckedChangeListener(
				new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				supportQueue = isChecked;
			}
		});
		
		((Button) findViewById(R.id.clear_button)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				for (TextView view: textViewsMap.values())
					view.setText("");
			}
		});
		
		
	}


	private void showToast(String str) {
		Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
	}

	public class ButtonListener implements OnClickListener {
		private String url;
		
		public ButtonListener(String url) {
			this.url = url;
		}
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(getBaseContext(), CustomService.class);
			intent.putExtra(CustomService.URL_PARAM, url);
		    intent.putExtra(CustomService.MESSENGER, messenger);
		    
			if (! supportQueue) 
				intent.putExtra("cancelAll", "true");

			startService(intent);
			
		}
		
	}
}
