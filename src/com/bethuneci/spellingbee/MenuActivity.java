package com.bethuneci.spellingbee;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

/* 
 * Author: Kent Chow
 * 
 * Date: May 27 2013
 * 
 * Description: This class is the screen that appears once the app is launched. A button exits
 * for the user to begin spelling words.
*/

public class MenuActivity extends Activity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
		
	}
	
	/* 
	 * Method used for initiating main activity
	*/
	public void startApp(View v) {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

}
