package com.nbos.phonebook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.nbos.phonebook.contentprovider.Provider;
import com.nbos.phonebook.database.IntCursorJoiner;
import com.nbos.phonebook.database.tables.BookTable;
import com.nbos.phonebook.database.tables.PicTable;
import com.nbos.phonebook.sync.Constants;
import com.nbos.phonebook.sync.client.Contact;
import com.nbos.phonebook.sync.client.ContactPicture;
import com.nbos.phonebook.sync.client.Group;
import com.nbos.phonebook.sync.client.PhoneContact;
import com.nbos.phonebook.sync.client.SharingBook;
import com.nbos.phonebook.sync.platform.BatchOperation;
import com.nbos.phonebook.sync.platform.SampleSyncAdapterColumns;
import com.nbos.phonebook.util.SimpleImageInfo;

public class Db {
	static String tag = "DATA";
	public static Cursor getContacts(Activity activity) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, null, null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}
	
	public static Cursor getGroups(ContentResolver cr) {
	    return cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, null,
	    		ContactsContract.Groups.DELETED + " = 0 ", null, null);
	}

	public static Cursor getContacts(Activity activity, String searchString) {
		return activity.managedQuery(ContactsContract.Contacts.CONTENT_URI, null, 
				ContactsContract.Data.DISPLAY_NAME+" like '" + searchString + "%'", null,
				ContactsContract.Contacts._ID);
				// ContactsContract.Contacts.DISPLAY_NAME);
	}

	public static Cursor getBook(Activity activity, String id) {
    	return activity.getContentResolver().query(
    			Uri.parse("content://"+Provider.AUTHORITY+"/"+Provider.BookContent.CONTENT_PATH),
	    		null,
	    		BookTable.BOOKID + "=" +id,
	    	    null, BookTable.CONTACTID);
	}
	
	public static void setGroupDirty(String groupId, ContentResolver cr) {
	    ContentValues values = new ContentValues();
	    values.put(ContactsContract.Groups.DIRTY, "1");

	    int num = cr.update(
	    		ContactsContract.Groups.CONTENT_URI, values,
	    		ContactsContract.Groups._ID + " = " + groupId, null);
	    Log.i(tag, "Updated "+num+" groups to dirty");

	}

	public static void addToGroup(String groupId, String rawContactId, ContentResolver cr) {
		   // this.removeFromGroup(personId, groupId);
			Log.i(tag, "Added contact to group: "+groupId+", contactId: "+rawContactId);
		    ContentValues values = new ContentValues();
		    values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
		            rawContactId);
		    values.put(
		            ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
		            groupId);
		    values
		            .put(
		                    ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE,
		                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

		    cr.insert(
		            ContactsContract.Data.CONTENT_URI, values);
		    Db.setGroupDirty(groupId, cr);		    
	}

	public static Cursor getContactsInGroup(String groupId,
			ContentResolver cr) {
	    return cr.query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.CONTACT_ID,
	    			ContactsContract.Data.RAW_CONTACT_ID,
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME,
	    			ContactsContract.CommonDataKinds.Photo.PHOTO
	    		},
	    	    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID+" = "+groupId,
	    	    null, ContactsContract.Data.CONTACT_ID);
	}
	
	public static String getAccountName(Context ctx) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(tag, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(tag, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(Constants.ACCOUNT_TYPE))
        		return account.name;
        }
        return null;
	}

	public static Account getAccount(Context ctx, String name) {
        Account[] accounts = AccountManager.get(ctx).getAccounts();
        Log.i(tag, "There are "+accounts.length+" accounts");
        for (Account account : accounts) 
        {
        	Log.i(tag, "account name: "+account.name+", type: "+account.type);
        	if(account.type.equals(Constants.ACCOUNT_TYPE) && account.name.equals(name))
        		return account;
        }
        return null;
	}

    public static void createAGroup(Context context, String groupName, String owner, String accountName, int id) {
    	// if(owner == null) owner = accountName;
        final BatchOperation batchOperation = new BatchOperation(context);
    	
		Log.i(tag, "Creating group: "+groupName);
		Uri mEntityUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_NAME, accountName)
			.appendQueryParameter(ContactsContract.Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE)
			.appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
			.build();
		
	
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(mEntityUri);
		Log.v("Group", "create accountgroup: "+Constants.ACCOUNT_TYPE+", "+accountName);
		builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, accountName);
		builder.withValue(ContactsContract.Groups.SYSTEM_ID, accountName);
		builder.withValue(ContactsContract.Groups.TITLE, groupName);
		builder.withValue(ContactsContract.Groups.SOURCE_ID, id);
		builder.withValue(ContactsContract.Groups.SYNC1, owner); // using sync1 for the owner of the shared book
		builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);
		batchOperation.add(builder.build());
		batchOperation.execute();
	}
    
    public static List<PhoneContact> getContacts(boolean newOnly, Context ctx) {
    	return PhoneContact.getContacts(newOnly, ctx);
    }
    
	public static List<PhoneContact> getContacts1(boolean newOnly, Context ctx) {
	    ContentResolver cr = ctx.getContentResolver();
	    
        final String[] CONTACTS_PROJECTION =
            new String[] {
        		ContactsContract.Contacts._ID, 
        		ContactsContract.Contacts.DISPLAY_NAME
        	};
        final String[] PHONES_PROJECTION =
                new String[] {
            		ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            		ContactsContract.CommonDataKinds.Phone.NUMBER
            	};


	    Cursor rawContactsCursor = getRawContactsCursor(cr, newOnly),
	    	contactsCursor = cr.query(ContactsContract.Contacts.CONTENT_URI, 
	    			CONTACTS_PROJECTION, null, null, null),
	    	phonesCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
	    			PHONES_PROJECTION, null, null, null),
	    	dataCursor = getData(ctx);
	    	// phonebookContactsCursor = cr.query(Constants.CONTACT_URI, null, null, null, null);
	    
	    Log.i(tag, "There are "+rawContactsCursor.getCount()+" contacts ");
	    List<PhoneContact> users = new ArrayList<PhoneContact>();
	    if(rawContactsCursor.getCount() == 0) return users;
	    rawContactsCursor.moveToFirst();
	    do {
	        String rawContactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts._ID)),
	        	contactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)),
	        	serverId = getServerIdFromContactId(dataCursor, contactId),
	        	dirty = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.DIRTY));
	        String name = getContactName(contactsCursor, contactId); 
	        String phoneNumber = getContactNumber(phonesCursor, contactId); 
	        Log.i(tag, "id: "+contactId+", rawContactId: "+rawContactId+", serverId: "+serverId+", name is: "+name+", number is: "+phoneNumber+", dirty: "+dirty);//+", accountName: "+accountName+", accountType: "+accountType);
	        if(name == null || phoneNumber == null) continue;
	        // users.add(new PhoneContact(name, phoneNumber, serverId, contactId, rawContactId));
	    } while(rawContactsCursor.moveToNext());
	    rawContactsCursor.close();
	    contactsCursor.close();
	    phonesCursor.close();
	    dataCursor.close();
	    return users;
	}
	
	public static List<ContactPicture> getContactPictures(Context ctx, boolean newOnly) {
		List<ContactPicture> pics = new ArrayList<ContactPicture>();
	    Cursor rawContactsCursor = getRawContactsCursor(ctx.getContentResolver(), newOnly),
	    	dataCursor = getData(ctx),
	    	photosDataCursor = ctx.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID, 
	    			ContactsContract.Data.CONTACT_ID,
	    			ContactsContract.Data.RAW_CONTACT_ID,
	    			ContactsContract.RawContacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME,
	    			ContactsContract.CommonDataKinds.Photo.PHOTO,
	    			Data.MIMETYPE, Data.DATA1,
	    		},
	    		ContactsContract.CommonDataKinds.Photo.PHOTO +" is not null",
	    	    null, ContactsContract.Data.CONTACT_ID);
	    
	    Log.i(tag, "There are "+rawContactsCursor.getCount()+" raw contacts entries for newOnly: "+newOnly);
	    Log.i(tag, "There are "+photosDataCursor.getCount()+" data entries");
	    
	    if(rawContactsCursor.getCount() == 0) return pics;
	    
	    photosDataCursor.moveToFirst();
	    rawContactsCursor.moveToFirst();
	    do {
	    	String contactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)),
	    		serverId = getServerIdFromContactId(dataCursor, contactId);
	    	ContactPicture pic = null;
			try {
				pic = getContactPicture(photosDataCursor, contactId, serverId);
				if(pic != null) pics.add(pic);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	
	    } while(rawContactsCursor.moveToNext());
		
	    rawContactsCursor.close();
	    photosDataCursor.close();
		return pics;
	}
	
	private static ContactPicture getContactPicture(Cursor dataCursor,
			String contactId, String serverId) throws IOException {
		if(dataCursor.getCount() == 0) return null;
		dataCursor.moveToFirst();
	    do {
	    	String cId = dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
	    	if(!cId.equals(contactId)) continue;
	    	String name = dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	    	byte[] pic = dataCursor.getBlob(dataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
	    	String contentType = findMimeTypeForImage(pic); 
	    		// dataCursor.getString(dataCursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.MIMETYPE));
	    	// String serverId
	    	Log.i(tag, "Contact["+contactId+"] "+name+", pic: "+(pic == null ? "null" : pic.length+", content type: "+contentType));
	    	return new ContactPicture(pic, serverId, contentType);
	    } while(dataCursor.moveToNext());
	    return null;
	}

    public static String findMimeTypeForImage(final byte[] bytes) throws IOException {
    	SimpleImageInfo info = new SimpleImageInfo(bytes);
    	return info.getMimeType();
    }

	public static Cursor getRawContactsCursor(ContentResolver cr, boolean newOnly) {
	    String where = newOnly ? ContactsContract.RawContacts.DIRTY + " = 1" : null;
        final String[] PROJECTION =
            new String[] {
        		ContactsContract.RawContacts._ID,
        		ContactsContract.RawContacts.CONTACT_ID, 
        		ContactsContract.RawContacts.DIRTY
        };
	    
        
		return cr.query(ContactsContract.RawContacts.CONTENT_URI, PROJECTION, where, null, ContactsContract.RawContacts._ID);	
	}

	private static String getContactNumber(Cursor phonesCursor, String contactId) {
		phonesCursor.moveToFirst();
		do {
			String cId = phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
			if(cId.equals(contactId))
				return phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		} while(phonesCursor.moveToNext());
		return null;
	}

	private static String getContactName(Cursor contactsCursor, String contactId) {
		contactsCursor.moveToFirst();
		do {
			String cId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID));
			if(cId.equals(contactId))
				return contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
		} while(contactsCursor.moveToNext());
		return null;
	}

	public static List<Group> getGroups(boolean newOnly, Context ctx) {
		List<Group> groups = new ArrayList<Group>();
	    ContentResolver cr = ctx.getContentResolver();
	    String where = ContactsContract.Groups.DELETED + " = 0 ";
	    if(newOnly)
	    	where += " and " + ContactsContract.Groups.DIRTY + " = 1 ";
	    Cursor groupsCursor = cr.query(ContactsContract.Groups.CONTENT_SUMMARY_URI, 
	    		new String [] {
	    			ContactsContract.Groups.TITLE,
	    			ContactsContract.Groups._ID,
	    			ContactsContract.Groups.SOURCE_ID,
	    			ContactsContract.Groups.ACCOUNT_NAME,
	    			ContactsContract.Groups.ACCOUNT_TYPE,
	    			ContactsContract.Groups.DIRTY
	    		},
	    		where, null, null);
	    Log.i(tag, "There are "+groupsCursor.getCount()+" groups");
	    Cursor contactsCursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
	    		// null,
	    	    new String[] {
	    			ContactsContract.Contacts._ID,
	    			ContactsContract.Contacts.DISPLAY_NAME
	    		},
	    		null, null, ContactsContract.Contacts._ID),
    	dataCursor = Db.getData(ctx);

	    Cursor phonesCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
        		new String[] {
	    			ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
	    			ContactsContract.CommonDataKinds.Phone.NUMBER
	    		}, 		
        		null,
        		null, null);

	    
	    Log.i(tag, "There are "+contactsCursor.getCount()+" contacts");
	    
	    while(groupsCursor.moveToNext())
	    {
	    	List<Contact> contacts = new ArrayList<Contact>();
	    	String name = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.TITLE));
	    	String groupId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups._ID));
	    	String groupSourceId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
	    	String dirty = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.DIRTY));
	    	String accName = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME));
	    	String accType = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));
	    	Log.i(tag, "Group: "+name+", account: "+accName+", account type: "+accType);
		    Cursor groupCursor = getContactsInGroup(new Integer(groupId).toString(), cr);
		    Log.i(tag, "There are "+groupCursor.getCount()+" contacts in group: "+groupId);
	    	
		    IntCursorJoiner joiner = new IntCursorJoiner(
		    		contactsCursor,
		    		new String[]
		    		{ContactsContract.Contacts._ID},
		    		groupCursor,
		    		new String[] {ContactsContract.Data.CONTACT_ID}
		    );
	        for (CursorJoiner.Result joinerResult : joiner) 
	        {
	        	switch (joinerResult) {
	        		case BOTH: // handle case where a row with the same key is in both cursors
	        			String contactId = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts._ID)),
	        				serverId = getServerIdFromContactId(dataCursor, contactId); 
	        				// contactName = contactsCursor.getString(contactsCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)),
	        				// contactNumber = getContactPhoneNumber(contactId, phonesCursor);
	        			// if(contactNumber == null) break;
	        			contacts.add(new Contact(serverId));
	        			Log.i(tag, "added contact: "+contactId+", serverId: "+serverId);//+", "+contactNumber+", "+contactName);
	        		break;
	        	}
	        }	    
	        groups.add(new Group(groupId, groupSourceId, name, null, contacts));
	        Log.i(tag, "dirty is "+dirty);
	        Log.i(tag, "Added group["+groupId+"] "+name+" with "+contacts.size()+" contacts");
	        groupCursor.close();
	    	// books
	    }
	    groupsCursor.close();
	    contactsCursor.close();
	    phonesCursor.close();
	    dataCursor.close();
	    return groups;
	}
	
	private static String getContactPhoneNumber(String contactId, Cursor phonesCursor) {
		phonesCursor.moveToFirst();
		do {
			String cId = phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
			if(!cId.equals(contactId)) continue;
			return phonesCursor.getString(phonesCursor
                    .getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
		} while(phonesCursor.moveToNext());
		return null;
	}

	public static List<SharingBook> getSharingBooks(boolean newOnly, Context ctx) {
    	List<SharingBook> books = new ArrayList<SharingBook>();
    	String where = newOnly ? BookTable.DIRTY + " is null" : null;
    	ContentResolver cr = ctx.getContentResolver();
    	Cursor cursor = cr.query(
    			Constants.SHARE_BOOK_URI, null, where, null, null),
    		dataCursor = getData(ctx),
    		groupsCursor = getGroups(cr);
    	if(cursor != null)
    		Log.i(tag, "There are "+cursor.getCount()+" contacts sharing books");
    	while(cursor.moveToNext())
    	{
    		String groupSourceId = getSourceIdFromGroupId(groupsCursor, 
    				cursor.getString(cursor.getColumnIndex(BookTable.BOOKID))),
    			contactSourceId = getServerIdFromContactId(dataCursor, 
    				cursor.getString(cursor.getColumnIndex(BookTable.CONTACTID)));
    		Log.i(tag, "groupSourceId: "+groupSourceId+", contactSourceId: "+contactSourceId);
    		if(groupSourceId != null && contactSourceId != null)
    			books.add(new SharingBook(groupSourceId, contactSourceId));
    	}
    	cursor.close();
    	dataCursor.close();
    	groupsCursor.close();
    	return books;
    }
	
	private static String getSourceIdFromGroupId(Cursor groupsCursor, String groupId) {
		if(groupsCursor.getCount() == 0) return null;
		groupsCursor.moveToFirst();
		do {
			String gId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups._ID)),
				sourceId = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.SOURCE_ID));
			if(gId.equals(groupId))
				return sourceId;
		}
		while(groupsCursor.moveToNext());
		return null;
	}

	public static String getServerIdFromContactId(Cursor dataCursor, String contactId) {
		dataCursor.moveToFirst();
		if(dataCursor.getCount() > 0)
		do {
			String mimeType = dataCursor.getString(dataCursor.getColumnIndex(Data.MIMETYPE));
			String cId = dataCursor.getString(dataCursor.getColumnIndex(Data.CONTACT_ID));
			if(!mimeType.equals(SampleSyncAdapterColumns.MIME_PROFILE)
			|| !cId.equals(contactId))
				continue;
			String serverId = dataCursor.getString(dataCursor.getColumnIndex(SampleSyncAdapterColumns.DATA_PID));
			// Log.i(TAG, "getServerIdFromContactId returning serverId: "+serverId+" for contactId: "+contactId);
			return serverId;
		} while(dataCursor.moveToNext()); 

		return null;
	}

	public static String getPhoneNumber(Context ctx) {
		String ph = ((TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		Log.i(tag, "Phone number is: "+ph);
		return ph;
    }

	public static void updateContactServerId(String contactId, String serverId, Context context, Cursor rawContactsCursor) {
		String rawContactId = getRawContactId(contactId, rawContactsCursor);
		Log.i(tag, "Raw contact id is: "+rawContactId);
		Uri uri = Data.CONTENT_URI;
		Cursor c = context.getContentResolver().query(uri, null,
				Data.CONTACT_ID + " = " + contactId + " and " +
				Data.MIMETYPE + " = '" + SampleSyncAdapterColumns.MIME_PROFILE + "'", null, null);
		ContentValues values = new ContentValues();
		if(c.getCount() == 0) { // insert
			Log.i(tag, "inserting");
            values.put(Data.MIMETYPE, SampleSyncAdapterColumns.MIME_PROFILE);
            values.put(SampleSyncAdapterColumns.DATA_PID, serverId);
            values.put(Data.RAW_CONTACT_ID, rawContactId);
            context.getContentResolver().insert(uri, values);
            c.close();
			return;
		}
		c.close();
		// update 
		Log.i(tag, "updating");
		values.put(SampleSyncAdapterColumns.DATA_PID, serverId);
		context.getContentResolver().update(uri, values, 
				Data.CONTACT_ID + " = " + contactId + " and " +
				Data.MIMETYPE + " = '" + SampleSyncAdapterColumns.MIME_PROFILE + "'", null);
	}

    public static Cursor getData(Context ctx) {
        final String[] PROJECTION =
            new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
                Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6, Data.DATA7, Data.DATA8, Data.DATA8, Data.DATA10, 
                Data.RAW_CONTACT_ID, Data.CONTACT_ID};

    	return ctx.getContentResolver().query(Data.CONTENT_URI, PROJECTION, null, null, null);
    }

	public static String getRawContactId(String contactId, Cursor rawContactsCursor) {
		// Log.i(tag, "getRawContactId("+contactId+"), rawContactsCursor size: "+rawContactsCursor.getCount());
		if(contactId == null || rawContactsCursor.getCount() == 0) return null;
		rawContactsCursor.moveToFirst();
		do {
			String cId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
			String rawContactId = rawContactsCursor.getString(rawContactsCursor.getColumnIndex(ContactsContract.RawContacts._ID));
			// Log.i(TAG, "checking: contactID: "+cId+", rawContactId: "+rawContactId);
			if(cId != null && cId.equals(contactId))
			{
				// Log.i(tag, "returning: "+rawContactId);
				return rawContactId;
			}
		} while(rawContactsCursor.moveToNext());
		Log.i(tag, "returning null");
		return null;
	}

	public static String getGroupNamesFromPhoneNumber(String phoneNumber, Context context) {
		Log.i(tag, "Getting groups for phone number: "+phoneNumber);
		if(phoneNumber == null) return null;
    	Cursor phonesCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
                new String[] {
        		ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        		ContactsContract.CommonDataKinds.Phone.NUMBER }, 
        		ContactsContract.CommonDataKinds.Phone.NUMBER +" = ? ", 
                new String[] { phoneNumber  }, null);
		Log.i(tag, "There are "+phonesCursor.getCount()+" contact entries");
		if(phonesCursor.getCount() == 0) return null;
		phonesCursor.moveToFirst();
		String contactId = phonesCursor.getString(phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

		Cursor contactGroupsCursor = context.getContentResolver()
    		.query(ContactsContract.Data.CONTENT_URI, 
	    	    new String[] {
    				ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, 
    				ContactsContract.Data.CONTACT_ID },
    				ContactsContract.Data.CONTACT_ID + " = ? "
    				+ " and "+ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE
    				+ " = ? ",
                new String[] { contactId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE }, 
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID);
    	Log.i(tag, "contactId: "+contactId+", is in "+contactGroupsCursor.getCount()+" groups");
    	if(contactGroupsCursor.getCount() == 0) return null;
    	contactGroupsCursor.moveToFirst();
    	String groupIdsIn = "(";
    	int count = 0, num = contactGroupsCursor.getCount();
    	do {
    		
    		String groupId = contactGroupsCursor.getString(contactGroupsCursor.getColumnIndex(
    				ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));
    		Log.i(tag, "Group id: "+groupId);
    		groupIdsIn += groupId;
    		if(count < num -1) 
    			groupIdsIn += ", ";
    		count++;
    			
    	} while(contactGroupsCursor.moveToNext());
    	groupIdsIn += ")";
    	Log.i(tag, "groups in = "+groupIdsIn);
    	Cursor groupsCursor = context.getContentResolver().query(
    			ContactsContract.Groups.CONTENT_URI, 
    			new String[] {
    				ContactsContract.Groups.TITLE
    			},
	    		ContactsContract.Groups.DELETED + " = 0 and " 
    			+ ContactsContract.Groups._ID + " in "+groupIdsIn, null, null);
    	Log.i(tag, "There are "+groupsCursor.getCount()+" groups");
    	if(groupsCursor.getCount() == 0) return null;
    	Set<String> groups = new HashSet<String>();
    	
    	 
    	groupsCursor.moveToFirst();
    	do {
    		String groupName = groupsCursor.getString(groupsCursor.getColumnIndex(ContactsContract.Groups.TITLE));
    		groups.add(groupName);
    	} while(groupsCursor.moveToNext());
    	
    	count = 0; num = groups.size();
    	String groupsString = "";
    	for(String g : groups) {
    		groupsString += g;
    		if(count < num - 1) groupsString += ", ";
    		count ++;
    	}
    	Log.i(tag, "return groups: "+groupsString);
		return groupsString;
	}

	public static void refreshAccount(Context ctx, String accountName) {
    	int num = ctx.getContentResolver().delete(Constants.PIC_URI, PicTable.ACCOUNT + " = ? ", new String[]{accountName});
    	Log.i(tag, "Deleted "+num+" pic entries");
	}
}