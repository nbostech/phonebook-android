package com.nbos.phonebook;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;

public class SharingWithActivity extends ListActivity {

	String tag = "SharingWithActivity",
		id, name;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.sharing_with);
	    
	    Bundle extras = getIntent().getExtras();
	    if(extras !=null)
	    {
	    	id = extras.getString("id");
	    	name = extras.getString("name");
	    }
	    
	    setTitle("Sharing "+name+" with");
	    populateContacts();
	}

	MatrixCursor m_cursor;
	private void populateContacts() {	
		
        Cursor contactsCursor = Data.getContacts(this);
        Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
		
        Cursor dataCursor = Data.getBook(this, id);
        Log.i(tag, "all columns: "+BookTable.ALL_COLUMNS.length+", data columns: "+dataCursor.getColumnCount());
        while(dataCursor.moveToNext())
        	Log.i(tag, "contactid: "+dataCursor.getString(dataCursor.getColumnIndex(BookTable.CONTACTID))
        			+" dirty: "+dataCursor.getString(dataCursor.getColumnIndex(BookTable.DIRTY))
        			+" serverId: "+dataCursor.getString(dataCursor.getColumnIndex(BookTable.SERVERID)));
        
        Log.i(tag, "Sharing with "+dataCursor.getCount()+" contacts");
	    IntCursorJoiner joiner = new IntCursorJoiner(
	    		contactsCursor, new String[] {ContactsContract.Contacts._ID},
	    		dataCursor,	new String[] {BookTable.CONTACTID}
	    );

        m_cursor = new MatrixCursor( 
            	new String[] {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},10);

        for (CursorJoiner.Result joinerResult : joiner) 
        {
        	String id;
        	switch (joinerResult) {
        		case BOTH: // handle case where a row with the same key is in both cursors
        			id = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
        			String name = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        			m_cursor.addRow(new String[] {id, name});        			
        		break;
        	}
        }	    
        Log.i(tag, "Sharing with "+m_cursor.getCount()+" contacts");
        String[] fields = new String[] {
                ContactsContract.Data.DISPLAY_NAME
        };
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.contact_entry, m_cursor,
                fields, new int[] {R.id.contact_name});
        getListView().setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.sharing_with_group_menu, menu);
	    return true;
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    	case R.id.add_contact_share_with:
	    		showAddContactsToShareWith();
	    		break;
	    }
	     return true;
    }

	static int SHARE_WITH = 1;
	private void showAddContactsToShareWith() {
		Intent i = new Intent(SharingWithActivity.this, SelectContactsToShareWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
        startActivityForResult(i, SHARE_WITH);	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == SHARE_WITH)
			populateContacts();
	}

}
