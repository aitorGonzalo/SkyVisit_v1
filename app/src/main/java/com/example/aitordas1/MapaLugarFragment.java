package com.example.aitordas1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapaLugarFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap gMap;
    private double lat = 0.0, lng = 0.0;
    private String nombreLugar = "Lugar";

    public MapaLugarFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa_lugar, container, false);

        // Verificar si existen argumentos y obtener coordenadas
        if (getArguments() != null) {
            lat = getArguments().getDouble("lat", 0.0);
            lng = getArguments().getDouble("lng", 0.0);
            nombreLugar = getArguments().getString("nombre", getString(R.string.nombre_lugar));
        }

        // Inicializar MapView
        mapView = view.findViewById(R.id.mapView);
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        if (lat != 0.0 && lng != 0.0) {
            LatLng lugar = new LatLng(lat, lng);
            gMap.addMarker(new MarkerOptions().position(lugar).title(nombreLugar));
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lugar, 15));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
