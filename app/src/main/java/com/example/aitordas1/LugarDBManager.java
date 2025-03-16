package com.example.aitordas1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class LugarDBManager {
    private SQLiteDatabase db;
    private LugarDBHelper dbHelper;

    public LugarDBManager(Context context) {
        dbHelper = new LugarDBHelper(context);
    }

    public void abrir() {
        if (db == null || !db.isOpen()) {
            db = dbHelper.getWritableDatabase();
            Log.d("LugarDBManager", "Base de datos abierta");
        }
    }

    public void cerrar() {
        if (db != null && db.isOpen()) {
            db.close();
            db = null; // Evitar accesos después de cerrar
            Log.d("LugarDBManager", "Base de datos cerrada");
        }
    }


    public void insertarLugar(String nombre, String descripcion, double latitud, double longitud) {
        ContentValues values = new ContentValues();
        values.put("nombre", nombre);
        values.put("descripcion", descripcion);
        values.put("latitud", latitud);
        values.put("longitud", longitud);
        db.insert("lugares", null, values);
    }

    public void eliminarLugar(int id) {
        db.delete("lugares", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Lugar> obtenerTodosLosLugares() {
        List<Lugar> lista = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM lugares", null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String nombre = cursor.getString(1);
            String descripcion = cursor.getString(2);
            double latitud = cursor.getDouble(3);
            double longitud = cursor.getDouble(4);
            lista.add(new Lugar(id, nombre, descripcion, latitud, longitud));
        }
        cursor.close();
        return lista;
    }

    public void actualizarLugar(int id, String nuevoNombre, String nuevaDescripcion, double nuevaLatitud, double nuevaLongitud) {
        ContentValues values = new ContentValues();
        values.put("nombre", nuevoNombre);
        values.put("descripcion", nuevaDescripcion);
        values.put("latitud", nuevaLatitud);
        values.put("longitud", nuevaLongitud);
        db.update("lugares", values, "id=?", new String[]{String.valueOf(id)});
    }


}

