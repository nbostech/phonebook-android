package com.nbos.phonebook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.util.ImageCursorAdapter;
import com.nbos.phonebook.value.ContactRow;

public class SharingWithActivity extends ListActivity {

	String tag = "SharingWithActivity", id, name,owner;
	List<String> ids;
	Cursor rawContactsCursor;
	ImageCursorAdapter adapter;
	MatrixCursor m_cursor;
	Db db;
	ListView listview;
	int layout;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		db = new Db(getApplicationContext());
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.sharing_with);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				android.R.drawable.ic_menu_share);

		registerForContextMenu(getListView());

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			id = extras.getString("id");
			name = extras.getString("name");
			layout = extras.getInt("layout");
		}

		setTitle("Group:" + " " + name + " " + "sharing with");
		populateContacts();
		listview = getListView();
		listview.setFastScrollEnabled(true);
		
		if(layout==R.layout.sharing_contact_entry){
		addExtraButtons();
		}
		
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the info on which item was selected
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		m_cursor.moveToPosition(info.position);

		String contactName = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

		menu.setHeaderTitle("Menu: " + contactName);

		menu.add(0, v.getId(), 0, "Stop share");
		menu.add(1, v.getId(), 0, "Permissions");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// Get the info on which item was selected
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		m_cursor.moveToPosition(info.position);
		String contactId = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts._ID)), name = m_cursor
				.getString(m_cursor
						.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

		Log.i(tag, "position is: " + info.position + ", contactId: "
				+ contactId + ", name: " + name);

		if (item.getTitle() == "Stop share") {
			Log.i(tag, "Remove: " + item.getItemId());
			// removeSharing(contactId);
		} else if (item.getTitle() == "Permissions") {
			sharingPermissions(contactId);
		} else {
			return false;
		}
		return true;
	}

	private void sharingPermissions(String contactId) {
		String contactName = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		Log.i(tag, "Permissions for " + contactName);
		Dialog dialog = new Dialog(SharingWithActivity.this);
		dialog.setContentView(R.layout.permissions);
		dialog.setTitle("Permissions for " + contactName);
		dialog.show();
	}

	private void removeSharing(String contactId) {

		try {

			Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.v(tag, e.getMessage(), e);
			Toast.makeText(this, tag + " Delete Failed", Toast.LENGTH_LONG)
					.show();
		}
	}

	public int getCheckedCount(ListView listview, int checkboxId) {

		int checkedCount = 0;
		for (int i = 0; i < listview.getChildCount(); i++) {
			View v = (View) listview.getChildAt(i);
			CheckBox checked = (CheckBox) v.findViewById(checkboxId);
			if (checked.isChecked()) {
				checkedCount++;
			}
		}
		return checkedCount;
	}

	private void removeSharing() {
		Toast.makeText(getApplicationContext(), "reomvesharing",
				Toast.LENGTH_LONG).show();

		for (int i = 0; i < listview.getChildCount(); i++) {
			View v = (View) listview.getChildAt(i);
			CheckBox check = (CheckBox) v.findViewById(R.id.check);
			if (check.isChecked()) {
				m_cursor.moveToPosition(i);
				String contactId = m_cursor.getString(m_cursor
						.getColumnIndex(ContactsContract.Contacts._ID)), name = m_cursor
						.getString(m_cursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

				Log.i(tag, "Id" + contactId + "name" + name);

			}

		}
		finish();
	}
     
	
	
	private void addExtraButtons() {
		
		
		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.linearLayout1);
		LinearLayout childLayout=(LinearLayout)mainLayout.findViewById(R.id.extraLayout);
	    Button stopSharing = new Button(this);		
		stopSharing.setId(R.id.stop_sharing);
		stopSharing.setText("Stop sharing");
		stopSharing.setWidth(500);
		childLayout.addView(stopSharing);

		stopSharing.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (getCheckedCount(listview, R.id.check) > 0){
					removeSharing();
				}
					
				else {
                       Toast.makeText(getApplicationContext(), "select contacts to stop sharing", Toast.LENGTH_LONG).show();
				}
			}
		});
	
		
	}

	
	
	
	
	
	private void populateContacts() {
		rawContactsCursor = db.getRawContactsCursor(false);
		Cursor contactsCursor = Db.getContacts(this);
		Log.i(tag, "There are " + contactsCursor.getCount() + " contacts");
		Cursor bookCursor = Db.getBook(this, id);
		Log.i(tag, "Book[" + id + "] has " + bookCursor.getCount()
				+ " contacts");
		ids = new ArrayList<String>();
		while (bookCursor.moveToNext())
			Log.i(tag,
					"contactid: "
							+ bookCursor.getString(bookCursor
									.getColumnIndex(BookTable.CONTACTID))
							+ " dirty: "
							+ bookCursor.getString(bookCursor
									.getColumnIndex(BookTable.DIRTY))
							+ " serverId: "
							+ bookCursor.getString(bookCursor
									.getColumnIndex(BookTable.SERVERID)));

		Log.i(tag, "Sharing with " + bookCursor.getCount() + " contacts");
		List<ContactRow> rows = new ArrayList<ContactRow>();
		Set<String> contactIds = new HashSet<String>();
		bookCursor.moveToFirst();
		if (bookCursor.getCount() > 0)
			do {
				String rawContactId = bookCursor.getString(bookCursor
						.getColumnIndex(BookTable.CONTACTID));
				ContactRow row = getContactRow(rawContactId, contactsCursor,
						contactIds);
				if (row != null)
					rows.add(row);
			} while (bookCursor.moveToNext());

		/*
		 * IntCursorJoiner joiner = new IntCursorJoiner( contactsCursor, new
		 * String[] {ContactsContract.Contacts._ID}, bookCursor, new String[]
		 * {BookTable.CONTACTID} );
		 */

		m_cursor = new MatrixCursor(new String[] {
				ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME }, 10);

		/*
		 * for (CursorJoiner.Result joinerResult : joiner) { String id; switch
		 * (joinerResult) { case BOTH: // handle case where a row with the same
		 * key is in both cursors id =
		 * contactsCursor.getString(contactsCursor.getColumnIndex
		 * (ContactsContract.Contacts._ID)); String name =
		 * contactsCursor.getString
		 * (contactsCursor.getColumnIndex(ContactsContract
		 * .Contacts.DISPLAY_NAME)); Log.i(tag, "name: "+name+", id: "+id);
		 * //m_cursor.addRow(new String[] {id, name}); if(name != null)
		 * rows.add(new ContactRow(id, name)); break; } }
		 */
		Collections.sort(rows);
		for (ContactRow row : rows) {
			m_cursor.addRow(new String[] { row.id, row.name });
			ids.add(row.id);
		}

		String[] fields = new String[] { ContactsContract.Data.DISPLAY_NAME };

		adapter = new ImageCursorAdapter(this, layout, m_cursor, ids, fields,
				new int[] { R.id.contact_name });
		getListView().setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		if (layout == (int) R.layout.contact_entry) {
			inflater.inflate(R.menu.sharing_with_group_menu, menu);
		} else {

		}
		return true;

	}

	@Override
	public void onAttachedToWindow() {

		super.onAttachedToWindow();
		openOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.add_contact_share_with:
			showAddContactsToShareWith();
			break;
		case R.id.add_new_contact:
			showCreateNewContact();
			break;
		case R.id.stop_sharing:
			removeSharing();
			break;
		}
		return true;
	}

	private void showCreateNewContact() {
		/*
		 * Intent intent = new
		 * Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT);
		 * intent.setData(ContactsContract.Contacts.CONTENT_URI);
		 * intent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
		 */
		Intent addContactIntent = new Intent(
				ContactsContract.Intents.Insert.ACTION);
		addContactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		startActivity(addContactIntent);
		/*
		 * Intent i = new Intent(SharingWithActivity.this,
		 * EditContactActivity.class); //
		 * i.setData(ContactsContract.Contacts.CONTENT_URI);
		 * i.setData(Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/0" ));
		 * startActivityForResult(i, INSERT_CONTACT_REQUEST);
		 */
		// this.getApplicationContext(), SharingWithActivity.class);
		// Intent intent = new Intent(Contacts.Intents.SHOW_OR_CREATE_CONTACT);
		// startActivity(intent);
		// startActivityForResult(intent, INSERT_CONTACT_REQUEST);

	}

	static int SHARE_WITH = 1, INSERT_CONTACT_REQUEST = 2;

	private void showAddContactsToShareWith() {
		Intent i = new Intent(SharingWithActivity.this,
				SelectContactsToShareWithActivity.class);
		i.putExtra("id", id);
		i.putExtra("name", name);
		startActivityForResult(i, SHARE_WITH);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SHARE_WITH)
			onAttachedToWindow();
		populateContacts();
		if (requestCode == INSERT_CONTACT_REQUEST)
			onAttachedToWindow();
		Log.i(tag, "Inserted contact");
	}

	private ContactRow getContactRow(String rawContactId,
			Cursor contactsCursor, Set<String> contactIds) {
		rawContactsCursor.moveToFirst();
		String contactId = null;
		if (rawContactsCursor.getCount() > 0)
			do {
				String rawId = rawContactsCursor.getString(rawContactsCursor
						.getColumnIndex(RawContacts._ID)), cId = rawContactsCursor
						.getString(rawContactsCursor
								.getColumnIndex(RawContacts.CONTACT_ID));
				if (!rawContactId.equals(rawId))
					continue;
				contactId = cId;
				break;
			} while (rawContactsCursor.moveToNext());

		contactsCursor.moveToFirst();
		if (contactId != null && contactsCursor.getCount() > 0)
			do {
				String cId = contactsCursor.getString(contactsCursor
						.getColumnIndex(Contacts._ID));
				if (!cId.equals(contactId) || contactIds.contains(contactId))
					continue;
				contactIds.add(contactId);
				String name = contactsCursor
						.getString(contactsCursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (name != null)
					return new ContactRow(contactId, name);
			} while (contactsCursor.moveToNext());
		return null;
	}

}
