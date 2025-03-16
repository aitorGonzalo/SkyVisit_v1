package com.example.aitordas1;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.example.aitordas1.R;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;


public class MainActivity extends AppCompatActivity {

    private LugarDBManager dbManager;
    private LugarAdapter adapter;
    private RecyclerView recyclerView;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_CODE_LOCATION = 100;
    private static final int REQUEST_CODE_NOTIFICATIONS = 1;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_LANGUAGE = "language";
    private static final int REQUEST_CODE_SAVE_FILE = 123; // Código para identificar la acción




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        aplicarTemaGuardado();
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate iniciado");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean cambioIdioma = prefs.getBoolean("cambio_idioma", false);

        // 🔹 Cargar el idioma guardado antes de inflar la vista
        cargarIdiomaGuardado();

        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "setContentView ejecutado");

        // 🔹 Configurar Toolbar correctamente
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setTitle(R.string.app_name); // Asegurar que el título se actualiza correctamente

        // 🔹 Forzar actualización del Toolbar en caso de cambio de idioma
        toolbar.requestLayout();
        toolbar.invalidate();

        configurarSwitchModoOscuro();
        crearCanalNotificaciones();

        // 🔹 Verificar si es un reinicio por cambio de idioma
        if (cambioIdioma) {
            Toast.makeText(this, getString(R.string.idioma_cambiado), Toast.LENGTH_SHORT).show();

            // Restablecer el flag para evitar que se muestre en cada inicio
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("cambio_idioma", false);
            editor.apply();
        } else {
            Toast.makeText(this, getString(R.string.app_iniciada_correctamente), Toast.LENGTH_LONG).show();
        }

        // 🔹 Inicializar la base de datos
        dbManager = new LugarDBManager(this);
        dbManager.abrir();

        recyclerView = findViewById(R.id.recyclerViewLugares);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 Cargar lugares desde la base de datos
        // En el método onCreate() después de abrir dbManager
        List<Lugar> lugares = dbManager.obtenerTodosLosLugares();

        // Añadir lugares con coordenadas (ejemplo de coordenadas aproximadas reales)
        if (!existeLugar("Estadio San Mames", lugares)) {
            dbManager.insertarLugar("Estadio San Mames", "Estadio del equipo Athletic Club de Bilbao", 43.2642, -2.9494);
        }
        if (!existeLugar("Gran Vía Bilbao", lugares)) {
            dbManager.insertarLugar("Gran Vía Bilbao", "Gran Vía - Avenidas de compras y ocio", 43.2619, -2.9340);
        }
        if (!existeLugar("Puente de Bizkaia", lugares)) {
            dbManager.insertarLugar("Puente de Bizkaia", "El conocido puente colgante", 43.3230, -3.0174);
        }
        if (!existeLugar("Teatro Arriaga", lugares)) {
            dbManager.insertarLugar("Teatro Arriaga", "Es la joya de las artes escénicas de Bilbao", 43.2594, -2.9245);
        }

        // Refrescar lista después de insertar
        lugares = dbManager.obtenerTodosLosLugares();
        adapter = new LugarAdapter(this, lugares);
        recyclerView.setAdapter(adapter);


        // 🔹 Botón para agregar un nuevo lugar
        FloatingActionButton fabAgregar = findViewById(R.id.fabAgregar);
        fabAgregar.setOnClickListener(view -> mostrarDialogoAgregarLugar());

        // 🔹 Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 🔹 Verificar permisos de notificación (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATIONS);
            }
        }

    }
    private boolean existeLugar(String nombre, List<Lugar> lista) {
        for (Lugar lugar : lista) {
            if (lugar.getNombre().equalsIgnoreCase(nombre)) {
                return true;
            }
        }
        return false;
    }
    // ✅ Método para aplicar el tema guardado
    private void aplicarTemaGuardado() {
        SharedPreferences sharedPreferences = getSharedPreferences("Ajustes", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("modo_oscuro", false);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // ✅ Método para configurar el switch de modo oscuro en la UI
    private void configurarSwitchModoOscuro() {
        // Cambia el tipo de Switch a SwitchMaterial
        com.google.android.material.switchmaterial.SwitchMaterial switchModoOscuro = findViewById(R.id.switchModoOscuro);
        SharedPreferences sharedPreferences = getSharedPreferences("Ajustes", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("modo_oscuro", false);
        switchModoOscuro.setChecked(isDarkMode);

        switchModoOscuro.setOnCheckedChangeListener((buttonView, isChecked) -> cambiarTema(isChecked));
    }

    // ✅ Método para cambiar de tema y guardar la preferencia
    public void cambiarTema(boolean activarModoOscuro) {
        SharedPreferences.Editor editor = getSharedPreferences("Ajustes", MODE_PRIVATE).edit();
        editor.putBoolean("modo_oscuro", activarModoOscuro);
        editor.apply();

        if (activarModoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_principal, menu);

        // 🔹 ACTUALIZAR EL TEXTO DEL MENÚ TRAS EL CAMBIO DE IDIOMA
        MenuItem item = menu.findItem(R.id.menu_idioma);
        if (item != null) {
            item.setTitle(getString(R.string.seleccionar_idioma)); // Aplica el idioma correcto
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId(); // Obtén el ID del ítem seleccionado

        if (id == R.id.menu_idioma) {
            mostrarDialogoSeleccionIdioma();
            return true;
        } else if (id == R.id.menu_acerca) {
            mostrarAcercaDe();
            return true;
        } else if (id == R.id.menu_exportar) { // 🔹 Se agregaron llaves para evitar errores de flujo
            seleccionarUbicacionYGuardar();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void mostrarAcercaDe() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.acerca_de); // Título desde recursos
        builder.setMessage(R.string.mensaje_acerca_de); // Mensaje desde recursos
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void seleccionarUbicacionYGuardar() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "lugares.txt"); // Nombre predeterminado
        startActivityForResult(intent, REQUEST_CODE_SAVE_FILE);
    }

    /**
     * 🔹 PASO 2: Captura el resultado del selector de archivos
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SAVE_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                guardarArchivoEn(uri);
            }
        }
    }

    /**
     * 🔹 PASO 3: Guarda el archivo en la ubicación elegida
     */
    private void guardarArchivoEn(Uri uri) {
        StringBuilder contenido = new StringBuilder();
        List<Lugar> lugares = dbManager.obtenerTodosLosLugares();
        for (Lugar lugar : lugares) {
            contenido.append("Nombre: ").append(lugar.getNombre()).append("\n");
            contenido.append("Descripción: ").append(lugar.getDescripcion()).append("\n\n");
        }

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            outputStream.write(contenido.toString().getBytes());
            outputStream.close();
            Toast.makeText(this, getString(R.string.archivo_guardado), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.error_guardar_archivo), Toast.LENGTH_SHORT).show();
        }

    }



    private void mostrarDialogoSeleccionIdioma() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.seleccionar_idioma));

        String[] idiomas = {getString(R.string.idioma_es), getString(R.string.idioma_en)};
        builder.setItems(idiomas, (dialog, which) -> {
            if (which == 0) {
                cambiarIdioma("es");
            } else {
                cambiarIdioma("en");
            }
        });

        builder.show();
    }


    private void cambiarIdioma(String idioma) {
        Locale nuevaLocale = new Locale(idioma);
        Locale.setDefault(nuevaLocale);

        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(nuevaLocale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LANGUAGE, idioma);
        editor.putBoolean("cambio_idioma", true);
        editor.apply();

        // 🔹 Evitar que el Toolbar se desconfigure
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.post(() -> {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
                params.topMargin = 20; // Ajusta este valor si lo necesitas
                toolbar.setLayoutParams(params);
                toolbar.requestLayout();
                toolbar.invalidate();
            });
        }

        // 🔹 Reiniciar solo la actividad sin cerrar la app
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }







    private void cargarIdiomaGuardado() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String idiomaGuardado = prefs.getString(KEY_LANGUAGE, "es"); // Español por defecto

        if (!getResources().getConfiguration().getLocales().get(0).getLanguage().equals(idiomaGuardado)) {
            cambiarIdioma(idiomaGuardado);
        }
    }

    private void mostrarDialogoAgregarLugar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.agregar_Lugar));

        final EditText inputNombre = new EditText(this);
        inputNombre.setHint(getString(R.string.nombre_lugar));
        inputNombre.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText inputDescripcion = new EditText(this);
        inputDescripcion.setHint(getString(R.string.descripcion_lugar));
        inputDescripcion.setInputType(InputType.TYPE_CLASS_TEXT);

        // Nuevos campos para latitud y longitud
        final EditText inputLatitud = new EditText(this);
        inputLatitud.setHint("Latitud (ej: 43.2630)");
        inputLatitud.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);

        final EditText inputLongitud = new EditText(this);
        inputLongitud.setHint("Longitud (ej: -2.9350)");
        inputLongitud.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(inputNombre);
        layout.addView(inputDescripcion);
        layout.addView(inputLatitud);  // nuevo campo latitud
        layout.addView(inputLongitud); // nuevo campo longitud

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.guardar), (dialog, which) -> {
            String nombre = inputNombre.getText().toString().trim();
            String descripcion = inputDescripcion.getText().toString().trim();
            String latitudStr = inputLatitud.getText().toString().trim();
            String longitudStr = inputLongitud.getText().toString().trim();

            if (!nombre.isEmpty() && !descripcion.isEmpty() && !latitudStr.isEmpty() && !longitudStr.isEmpty()) {
                try {
                    double latitud = Double.parseDouble(latitudStr);
                    double longitud = Double.parseDouble(longitudStr);

                    dbManager.insertarLugar(nombre, descripcion, latitud, longitud);
                    actualizarListaLugares();

                    mostrarNotificacion(nombre);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Latitud/Longitud inválidas", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.cancelar), (dialog, which) -> dialog.cancel());

        builder.show();
    }





    private void actualizarListaLugares() {
        List<Lugar> lugares = dbManager.obtenerTodosLosLugares();
        adapter = new LugarAdapter(this, lugares);
        recyclerView.setAdapter(adapter);
    }



    @Override
    protected void onStop() {
        super.onStop();
        Log.d("MainActivity", "onStop() ejecutado, la base de datos NO se cerrará aquí");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.cerrar();
            Log.d("MainActivity", "Base de datos cerrada en onDestroy()");
        }
    }




    private void crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager elManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel elCanal = new NotificationChannel("IdCanal", "Canal Lugares",
                    NotificationManager.IMPORTANCE_DEFAULT);

            elCanal.setDescription("Canal para notificaciones de nuevos lugares");
            elCanal.enableLights(true);
            elCanal.setLightColor(Color.RED);
            elCanal.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            elCanal.enableVibration(true);

            elManager.createNotificationChannel(elCanal);
        }
    }

    private void mostrarNotificacion(String nombreLugar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent intentEnNot = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder elBuilder = new NotificationCompat.Builder(this, "IdCanal")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                .setContentTitle(getString(R.string.nuevo_lugar))
                .setContentText(getString(R.string.se_ha_agregado) + nombreLugar)
                .setSubText(getString(R.string.abrir_app))
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setAutoCancel(true)
                .setContentIntent(intentEnNot);

        NotificationManagerCompat elManager = NotificationManagerCompat.from(this);
        elManager.notify(1, elBuilder.build());
    }



}
