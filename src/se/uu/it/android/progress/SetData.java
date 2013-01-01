package se.uu.it.android.progress;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class SetData extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "sets.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_NAME = "sets";

	public static final String _ID = BaseColumns._ID;
	public static final String NAME = "name";
	public static final String ACTIVE_TIME = "active_time";
	public static final String PASSIVE_TIME = "passive_time";
	public static final String REPETITIONS = "repetitions";

	public SetData(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public SetData(Context context, String name, CursorFactory factory,
			int version, DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// CREATE TABLE sets (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, active_time INTEGER, passive_time INTEGER, repetitions INTEGER);
		String sql =
				"CREATE TABLE " + TABLE_NAME + " ("
						+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ NAME + " TEXT NOT NULL, "
						+ ACTIVE_TIME + " INTEGER, "
						+ PASSIVE_TIME + " INTEGER, "
						+ REPETITIONS + " INTEGER"
						+ ");";

		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public void insert(String name, int activeTime, int passiveTime, int repetitions) {
		SQLiteDatabase db = getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(NAME, name);
		values.put(ACTIVE_TIME, activeTime);
		values.put(PASSIVE_TIME, passiveTime);
		values.put(REPETITIONS, repetitions);

		db.insertOrThrow(TABLE_NAME, null, values);
	}
	
	public void remove(String name) {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_NAME, "NAME LIKE \"" + name + "\"", null);
	}

	@SuppressWarnings("deprecation")
	public Cursor all(Activity activity) {
		String[] from = { _ID, NAME, ACTIVE_TIME, PASSIVE_TIME, REPETITIONS };
		String order = NAME;

		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, from, null, null, null, null, order);
		activity.startManagingCursor(cursor);

		return cursor;
	}

	public long count() {
		SQLiteDatabase db = getReadableDatabase();
		return DatabaseUtils.queryNumEntries(db, TABLE_NAME);
	}

}