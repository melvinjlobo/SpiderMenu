package com.abysmel.spidermenu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SpiderMenu.SpiderMenuClickListener {
	SpiderMenu spiderMenu;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		spiderMenu = (SpiderMenu) findViewById( R.id.spidermenu );
		spiderMenu.setSpiderMenuClickListener( this );
	}

	@Override
	public void onSpiderMenuClick( int nID ) {
		Logger.d( "View clicked!!!");
		String clickText = "Dunno what is clicked";
		switch ( nID ) {
			case R.id.menuitem1:
				clickText = "Car";
				break;
			case R.id.menuitem2:
				clickText = "Cloud";
				break;
			case R.id.menuitem3:
				clickText = "Mountain";
				break;
			case R.id.menuitem4:
				clickText = "Sun";
				break;
			case R.id.menuitem5:
				clickText = "Trees";
				break;
			case R.id.menuitem6:
				clickText = "Camera";
				break;
		}

		Toast.makeText( this, clickText + " Clicked!", Toast.LENGTH_SHORT ).show();
	}
}
