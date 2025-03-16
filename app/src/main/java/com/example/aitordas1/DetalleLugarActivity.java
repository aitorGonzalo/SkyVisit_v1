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

import java.util.Locale;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_lugar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Obtener datos del Intent
        nombreLugar = getIntent().getStringExtra("nombre");
        descripcionLugar = getIntent().getStringExtra("descripcion");
        lugarLat = getIntent().getDoubleExtra("latitud", 0.0);
        lugarLng = getIntent().getDoubleExtra("longitud", 0.0);
        cargarFragments();
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

        // Logs para depurar valores de coordenadas
        Log.d("DistanciaDebug", "🔹 Usuario Lat: " + usuarioLat + ", Usuario Lng: " + usuarioLng);
        Log.d("DistanciaDebug", "📍 Lugar Lat: " + lugarLat + ", Lugar Lng: " + lugarLng);

        double distanciaKm = calcularDistanciaHaversine(usuarioLat, usuarioLng, lugarLat, lugarLng);
        Log.d("DistanciaDebug", "📏 Distancia calculada: " + distanciaKm + " km");

        String distanciaTexto = String.format(Locale.getDefault(), "%.2f km", distanciaKm);

        // Fragmento de información
        InfoLugarFragment infoFragment = new InfoLugarFragment();
        Bundle infoArgs = new Bundle();
        infoArgs.putString("nombre", nombreLugar);
        infoArgs.putString("descripcion", descripcionLugar);
        infoArgs.putString("distancia", distanciaTexto);
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
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int RADIO_TIERRA = 6371; // Radio aproximado de la tierra en kilómetros

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distancia = RADIO_TIERRA * c;

        // Log para confirmar el resultado intermedio
        Log.d("DistanciaDebug", "✅ Resultado interno Haversine: " + distancia + " km");

        return distancia;
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
        calcularDistanciaHaversine(usuarioLat, usuarioLng, lugarLat, lugarLng);
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
