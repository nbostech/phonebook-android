package com.nbos.phonebook;

import java.io.IOException;
import java.util.List;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.authenticator.AuthenticatorActivity;
import com.nbos.phonebook.sync.syncadapter.SyncAdapter;
import com.nbos.phonebook.util.Notify;
import com.nbos.phonebook.util.WelcomeActivityCursorAdapter;
import com.nbos.phonebook.value.Contact;
import com.nbos.phonebook.value.Group;

public class WelcomeActivity extends ListActivity {
	static String tag = "Phonebook";
	List<Contact> m_contacts = null;
	List<Contact> m_phoneContacts = null;
	ProgressDialog m_ProgressDialog = null;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.main);
		setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.icon);
		testContentProvider();
		populateGroups();
		getListView().setTextFilterEnabled(true);
		String phoneNumber = getPhoneNumber();
		Log.i(tag, "phone number: " + phoneNumber);

		// Test.deleteContactsServerData(getApplicationContext());
		// Test.getContactServerData(getApplicationContext());
		// Log.i(tag, "WelcomeActivity");
		// Test.getGroups(getApplicationContext());
		// PhoneContact.getContacts(false, getApplicationContext());
		// Test.resetDirtyContacts(getApplicationContext());
		// Test.getRawContacts(getApplicationContext());
		// Test.getPics(getApplicationContext());
		// Test.getContacts(this.getApplicationContext());
		// Test.getGroups(this.getApplicationContext());
		// Test.getContacts(this.getApplicationContext());
		// Test.telephony(this);
		if (!hasAccount(Constants.ACCOUNT_TYPE)) {
			final Intent intent = new Intent(getApplicationContext(),
					AuthenticatorActivity.class);
			intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE,
					Constants.ACCOUNT_TYPE);
			startActivity(intent);
		}
		// startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS));

	}


	@Override 
	public boolean onCreateOptionsMenu(Menu menu) { 
		MenuInflater inflater = getMenuInflater(); 
		inflater.inflate(R.menu.group_list_menu, menu); 
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
			case R.id.add_group: 
				Intent i = new Intent(WelcomeActivity.this, AddGroupActivity.class);
		 		startActivityForResult(i, ADD_GROUP); 
		 	break;
			case R.id.sync:
			try {
				doSync();
			} catch (Exception e) {
				e.printStackTrace();
			} 
		 	break;
			case R.id.delete_group:{
				Intent d=new Intent(WelcomeActivity.this, DeleteGroupActivity.class);
				startActivityForResult(d, DELETE_GROUP);
			break;
				//showDeleteGroupDialog();
			}

		} 
		return true;
	}
	
	private void doSync() {
		SyncAdapter syncAdapter = new SyncAdapter(getApplicationContext(), true);
		Account account = getAccount();
		Log.i(tag, "Account is: "+account);//.name+", "+account.type);
		if(account != null)
			syncAdapter.onPerformSync(account, null, null, null, null);
		else
			Toast.makeText(getApplicationContext(), "You have not added a phonebook account", Toast.LENGTH_LONG)
				.show();
	}

	static int ADD_GROUP = 1, SHOW_GROUP = 2, DELETE_GROUP=3;

	/*public boolean onClickAddGroup(View v) {

		// Handle item selection
		switch (v.getId()) {
		case R.id.add_group_new:
			Intent i = new Intent(WelcomeActivity.this, AddGroupActivity.class);
			startActivityForResult(i, ADD_GROUP);
			break;
		}

		return true;

	}*/

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == ADD_GROUP && resultCode == RESULT_OK)
				|| (requestCode == SHOW_GROUP && resultCode == RESULT_OK) ||(requestCode == DELETE_GROUP && resultCode == RESULT_OK))
		
			refreshGroups();
	}

	private void refreshGroups() {
		System.out.println("Refresh the groups");
		m_cursor.requery();
	}

	List<Group> m_groups;

	private void testContentProvider() {
		Uri URI = Uri.parse("content://" + Provider.AUTHORITY + "/"
				+ Provider.BookContent.CONTENT_PATH);
		Cursor c = getContentResolver().query(URI, null, null, null, null);
		Log.i(tag, "There are " + c.getCount() + " books");
	}

	Cursor m_cursor;

	private void populateGroups() {
		ContentResolver cr = getContentResolver();
		m_cursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
				ContactsContract.Groups.DELETED + "=0", null,
				ContactsContract.Groups.TITLE);

		String[] fields = new String[] { ContactsContract.Groups.TITLE,
				ContactsContract.Groups.SUMMARY_COUNT,
				ContactsContract.Groups.SYNC1 };
		Cursor sharedBooksCursor = Db.getBooks(cr);
		WelcomeActivityCursorAdapter adapter = new WelcomeActivityCursorAdapter(
				this, R.layout.group_entry, m_cursor, sharedBooksCursor,
				fields, new int[] { R.id.groupName, R.id.groupCount,
						R.id.groupOwner });

		adapter.setStringConversionColumn(m_cursor
				.getColumnIndexOrThrow(ContactsContract.Groups.TITLE));

		adapter.setFilterQueryProvider(new FilterQueryProvider() {

			public Cursor runQuery(CharSequence constraint) {
				String partialItemName = null;
				if (constraint != null) {
					partialItemName = constraint.toString();
				}
				m_cursor = getContentResolver().query(
						ContactsContract.Groups.CONTENT_SUMMARY_URI,
						null,
						// "DISPLAY_NAME = '" + NAME + "'",
						ContactsContract.Groups.DELETED + "=0" + " and "
								+ ContactsContract.Groups.TITLE + " like '%"
								+ partialItemName + "%'", null, null);

				Log.i(tag, "partial name is" + partialItemName);

				return m_cursor;
			}
		});

		getListView().setAdapter(adapter);

	}

	ListView mGroupList;

	private Runnable mainUiThread = new Runnable() {
		public void run() {
			// createAGroup(getApplicationContext());

			/*
			 * m_phoneContacts = getPhoneContacts(); TextView note2 = (TextView)
			 * findViewById(R.id.note2);
			 * note2.setText("There are "+m_phoneContacts
			 * .size()+" contacts in your phone"); m_ProgressDialog =
			 * ProgressDialog.show(WelcomeActivity.this, "Please wait ...",
			 * "Retrieving data ...", true); new
			 * DownloadContactsTask().execute();
			 */
		}

		boolean hasAccount(String name, String type) {
			Account[] accounts = AccountManager.get(getApplicationContext())
					.getAccounts();
			Log.i(tag, "There are " + accounts.length + " accounts");
			for (Account account : accounts)
				if (account.name == name && account.type == type)
					return true;
			return false;
		}

		private void getAccounts() {
			Account[] accounts = AccountManager.get(getApplicationContext())
					.getAccounts();
			Log.i(tag, "There are " + accounts.length + " accounts");
			for (Account account : accounts) {
				Log.i(tag, "account name: " + account.name);
				Log.i(tag, "account type: " + account.type);
		}

		}
	};

	String m_exception = null;

	private Runnable returnResults = new Runnable() {
		public void run() {
			/*
			 * if(m_orders != null && m_orders.size() > 0){
			 * m_adapter.notifyDataSetChanged(); for(int
			 * i=0;i<m_orders.size();i++) m_adapter.add(m_orders.get(i)); }
			 */

			// m_adapter.notifyDataSetChanged();
		}
	};

	private String getPhoneNumber() {
		return ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
				.getLine1Number();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		m_cursor.moveToPosition(position);
		String groupId = m_cursor.getString(m_cursor
				.getColumnIndex(ContactsContract.Groups._ID)), groupName = m_cursor
				.getString(m_cursor
						.getColumnIndex(ContactsContract.Groups.TITLE)), groupOwner = m_cursor
				.getString(m_cursor
						.getColumnIndex(ContactsContract.Groups.SYNC1));

		Log.i(tag, "Group id: " + groupId + ", name: " + groupName);
		Intent i = new Intent(WelcomeActivity.this, GroupActivity.class);
		i.putExtra("id", groupId);
		i.putExtra("name", groupName);
		i.putExtra("owner", groupOwner);
		startActivityForResult(i, SHOW_GROUP);
	}

	boolean hasAccount(String type) {
		Account[] accounts = AccountManager.get(getApplicationContext())
				.getAccounts();
		Log.i(tag, "There are " + accounts.length + " accounts");
		for (Account account : accounts)
			if (account.type.equals(type))
				return true;
		return false;
	}
	
	private Account getAccount() {
		Account[] accounts = AccountManager.get(getApplicationContext())
				.getAccounts();
		Log.i(tag, "There are " + accounts.length + " accounts");
		for (Account account : accounts) 
			if(Constants.ACCOUNT_TYPE.equals(account.type))
				return account;
		return null;
	}
	
	
	
	
}

