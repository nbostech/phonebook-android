package com.nbos.phonebook.util;

import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nbos.phonebook.Db;
import com.nbos.phonebook.R;

public class ImageCursorAdapter extends SimpleCursorAdapter implements SectionIndexer{

	private Cursor c;
	private Context context;
	List<String> ids;
	int layout;
	String tag="SelectContactsToShareWith";
	AlphabetIndexer alphaIndexer; 

	public ImageCursorAdapter(Context context, int layout, Cursor c,
			List<String> ids, String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.c = c;
		this.context = context;
		this.ids = ids;
		this.layout = layout;
		alphaIndexer=new AlphabetIndexer(c, c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME), " ABCDEFGHIJKLMNOPQRSTUVWXYZ"); 
	}

	public void setCursor(Cursor c) {
		this.c = c;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
	
	
	

	@Override
	public View getView(int pos, View inView, ViewGroup parent) {
		View v = inView;
		
		if (v == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(layout, null);
		}
		this.c.moveToPosition(pos);
		
		
		// ContactsContract.Contacts._ID,
		// ContactsContract.Contacts.DISPLAY_NAME,
		// ContactsContract.CommonDataKinds.Photo.PHOTO}, 10);
		String contactName = this.c.getString(this.c
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		
		// byte[] pic = images.get(pos);// this.c.getBlob(this.c.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
		ImageView iv = (ImageView) v.findViewById(R.id.contact_pic);
		iv.setImageBitmap(Db.getPhoto(context.getContentResolver(), ids.get(pos)));
		iv.setScaleType(ScaleType.FIT_XY);
	
		/*if (pic == null) 
			iv.setImageBitmap(null);
		else
		{
			iv.setImageBitmap(BitmapFactory.decodeByteArray(pic, 0, pic.length));
			iv.setScaleType(ScaleType.FIT_XY);
		}*/
		TextView cName = (TextView) v.findViewById(R.id.contact_name);
		cName.setText(contactName);
		
		return v;
	}

	public int getPositionForSection(int section) {
		
		return alphaIndexer.getPositionForSection(section);
	}

	public int getSectionForPosition(int position) {
		
		return alphaIndexer.getSectionForPosition(position); 
	}

	public Object[] getSections() {
		
		return alphaIndexer.getSections();
	}

	
}
