package com.example.aitordas1;

import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

public class DetalleLugarActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap gMap;
    private TextView textNombreLugar, textDescripcionLugar, textDistancia;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private double lugarLat = 0.0, lugarLng = 0.0;
    private double usuarioLat = 43.2630;
    private double usuarioLng = -2.9350;
    private String nombreLugar, descripcionLugar;
    private static final String API_KEY = "AIzaSyDunAusn35nQCpqlpcROZyjkcPujMlwolw";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_lugar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtener datos del Intent
        nombreLugar = getIntent().getStringExtra("nombre");
        descripcionLugar = getIntent().getStringExtra("descripcion");

        // Obtener coordenadas del lugar antes de cargar los fragments
        obtenerCoordenadasLugar();

        // Configurar botones
        Button btnAbrirMapa = findViewById(R.id.btnAbrirMapa);
        if (btnAbrirMapa != null) {
            btnAbrirMapa.setOnClickListener(v -> abrirEnGoogleMaps());
        }

        Button btnCompartirUbicacion = findViewById(R.id.btnCompartirUbicacion);
        if (btnCompartirUbicacion != null) {
            btnCompartirUbicacion.setOnClickListener(v -> compartirUbicacion());
        }
    }




    private void cargarFragments() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Fragmento de información
        InfoLugarFragment infoFragment = new InfoLugarFragment();
        Bundle infoArgs = new Bundle();
        infoArgs.putString("nombre", nombreLugar);
        infoArgs.putString("descripcion", descripcionLugar);
        infoArgs.putString("distancia", "Calculando..."); // Inicializa la distancia
        infoFragment.setArguments(infoArgs);
        transaction.replace(R.id.fragment_info_container, infoFragment);

        // Fragmento del mapa
        MapaLugarFragment mapaFragment = new MapaLugarFragment();
        Bundle mapaArgs = new Bundle();
        mapaArgs.putDouble("lat", lugarLat);
        mapaArgs.putDouble("lng", lugarLng);
        mapaArgs.putString("nombre", nombreLugar);
        mapaFragment.setArguments(mapaArgs);
        transaction.replace(R.id.fragment_mapa_container, mapaFragment);

        transaction.commit();
    }


    private void abrirEnGoogleMaps() {
        if (lugarLat != 0.0 && lugarLng != 0.0) {
            Uri gmmIntentUri = Uri.parse("geo:" + lugarLat + "," + lugarLng + "?q=" + Uri.encode(nombreLugar));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(this, getString(R.string.google_maps_no_instalado), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error al abrir Google Maps. Asegúrese de usar un emulador con imagen oficial que incluya Google Play Services.", Toast.LENGTH_LONG).show();
                Log.e("DetalleLugarActivity", "Error al abrir Google Maps", e);
            }
        } else {
            Toast.makeText(this, getString(R.string.ubicacion_no_disponible), Toast.LENGTH_SHORT).show();
        }
    }

    private void compartirUbicacion() {
        if (lugarLat != 0.0 && lugarLng != 0.0) {
            String urlMaps = "https://www.google.com/maps/search/?api=1&query=" + lugarLat + "," + lugarLng;

            Intent compartirIntent = new Intent(Intent.ACTION_SEND);
            compartirIntent.setType("text/plain");
            compartirIntent.putExtra(Intent.EXTRA_TEXT, urlMaps); // 🔥 SOLO el enlace

            startActivity(Intent.createChooser(compartirIntent, getString(R.string.compartir_ubicacion)));
        } else {
            Toast.makeText(this, getString(R.string.error_ubicacion_no_disponible), Toast.LENGTH_SHORT).show();
        }
    }


    private void obtenerCoordenadasLugar() {
        if (nombreLugar == null || nombreLugar.trim().isEmpty()) {
            Log.e("GeocodingError", getString(R.string.error_nombre_lugar_invalido));
            runOnUiThread(() ->
                    Toast.makeText(this, getString(R.string.error_nombre_lugar_invalido), Toast.LENGTH_SHORT).show()
            );
            return;
        }

        new Thread(() -> {
            try {
                String urlString = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
                        nombreLugar.replace(" ", "+") + "&key=" + API_KEY;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    Log.e("GeocodingError", getString(R.string.error_api_geocoding) + ": " + responseCode);
                    runOnUiThread(() -> mostrarDialogoError(
                            getString(R.string.error_api_geocoding),
                            getString(R.string.mensaje_error_api) + "\n" + getString(R.string.error_codigo) + responseCode
                    ));
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray results = jsonResponse.optJSONArray("results");

                if (results != null && results.length() > 0) {
                    JSONObject location = results.getJSONObject(0)
                            .getJSONObject("geometry").getJSONObject("location");
                    lugarLat = location.getDouble("lat");
                    lugarLng = location.getDouble("lng");

                    Log.d("GeocodingAPI", "Coordenadas obtenidas: Lat=" + lugarLat + ", Lng=" + lugarLng);

                    runOnUiThread(() -> {
                        cargarFragments();
                        calcularDistanciaGoogle(); // 🔥 Agregado para calcular la distancia después de obtener las coordenadas
                    });

                } else {
                    Log.e("GeocodingError", getString(R.string.error_ubicacion_no_encontrada, nombreLugar));
                    runOnUiThread(() ->
                            Toast.makeText(this, getString(R.string.error_ubicacion_no_encontrada, nombreLugar), Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e("GeocodingError", getString(R.string.error_ubicacion_no_disponible), e);
                String mensajeError = e instanceof java.net.UnknownHostException ? getString(R.string.error_conexion)
                        : getString(R.string.error_ubicacion_no_disponible) + ": " + e.getMessage();

                runOnUiThread(() -> mostrarDialogoError(getString(R.string.error_ubicacion_no_disponible), mensajeError));
            }

        }).start();
    }



    private void mostrarDialogoError(String titulo, String mensaje) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(titulo)
                    .setMessage(mensaje)
                    .setPositiveButton("Aceptar", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)
                    .show();
        });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        if (lugarLat != 0.0 && lugarLng != 0.0) {
            LatLng lugar = new LatLng(lugarLat, lugarLng);
            gMap.addMarker(new MarkerOptions().position(lugar).title(nombreLugar));
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lugar, 15));
        } else {
            Toast.makeText(this, "Coordenadas no disponibles", Toast.LENGTH_SHORT).show();
        }
    }

    private void obtenerUbicacionUsuario() {
        Log.d("UbicacionUsuario", "Se omite la obtención de ubicación, usando Bilbao como ubicación fija.");
        calcularDistanciaGoogle();
        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        usuarioLat = location.getLatitude();
                        usuarioLng = location.getLongitude();
                        Log.d("UbicacionUsuario", "Latitud usuario: " + usuarioLat + ", Longitud usuario: " + usuarioLng);
                        calcularDistanciaGoogle();
                    } else {
                        Log.w("UbicacionUsuario", "No se pudo obtener la última ubicación");
                        runOnUiThread(() -> textDistancia.setText("Ubicación no disponible"));
                    }
                }).addOnFailureListener(e -> {
                    Log.e("UbicacionError", "Error obteniendo última ubicación", e);
                    runOnUiThread(() -> textDistancia.setText("Error al obtener ubicación"));
                });*/

    }




    private void calcularDistanciaGoogle() {
        Log.d("UbicacionUsuario", "Latitud usuario: " + usuarioLat + ", Longitud usuario: " + usuarioLng);
        Log.d("UbicacionDestino", "Latitud destino: " + lugarLat + ", Longitud destino: " + lugarLng);

        if (usuarioLat == 0.0 || usuarioLng == 0.0 || lugarLat == 0.0 || lugarLng == 0.0) {
            Log.e("DistanceMatrixError", "Coordenadas no disponibles");
            actualizarDistanciaEnFragmento("Distancia no disponible");
            return;
        }

        new Thread(() -> {
            try {
                String urlString = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                        usuarioLat + "," + usuarioLng + "&destinations=" + lugarLat + "," + lugarLng +
                        "&mode=driving&key=" + API_KEY;

                Log.d("DistanceMatrixAPI", "URL: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                Log.d("DistanceMatrixAPI", "Respuesta JSON: " + jsonResponse.toString());

                JSONArray rows = jsonResponse.optJSONArray("rows");

                if (rows != null && rows.length() > 0) {
                    JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                    if (elements.has("distance")) {
                        String distanciaTexto = elements.getJSONObject("distance").getString("text");
                        actualizarDistanciaEnFragmento("Distancia: " + distanciaTexto);
                    } else {
                        actualizarDistanciaEnFragmento("Distancia no disponible");
                    }
                } else {
                    Log.e("DistanceMatrixError", "No se pudo obtener la distancia");
                    actualizarDistanciaEnFragmento("Distancia no disponible");
                }
            } catch (Exception e) {
                Log.e("DistanceMatrixError", "Error obteniendo distancia", e);
                actualizarDistanciaEnFragmento("Error al obtener distancia");
            }
        }).start();
    }

    // Método para actualizar la distancia en el fragmento
    private void actualizarDistanciaEnFragmento(String distancia) {
        runOnUiThread(() -> {
            InfoLugarFragment infoFragment = (InfoLugarFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_info_container);
            if (infoFragment != null) {
                infoFragment.actualizarDistancia(distancia);
            }
        });
    }






    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerUbicacionUsuario();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        cargarFragments(); // Recargar fragments sin reiniciar la actividad
    }

}
