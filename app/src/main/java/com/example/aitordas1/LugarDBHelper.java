package com.example.aitordas1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LugarDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "lugares.db";
    private static final int DATABASE_VERSION = 1;

    // Sentencia SQL para crear la tabla
    private static final String CREATE_TABLE_LUGARES =
            "CREATE TABLE lugares (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nombre TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL)";

    public LugarDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_LUGARES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS lugares");
        onCreate(db);
    }
}

