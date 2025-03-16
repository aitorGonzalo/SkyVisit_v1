package com.example.aitordas1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
public class InfoLugarFragment extends Fragment {

    private TextView textNombreLugar, textDescripcionLugar, textDistancia;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_info_lugar, container, false);

        textNombreLugar = view.findViewById(R.id.textNombreLugar);
        textDescripcionLugar = view.findViewById(R.id.textDescripcionLugar);
        textDistancia = view.findViewById(R.id.textDistancia);

        // Obtener datos del Bundle
        if (getArguments() != null) {
            textNombreLugar.setText(getArguments().getString("nombre", "Sin nombre"));
            textDescripcionLugar.setText(getArguments().getString("descripcion", "Sin descripción"));
            textDistancia.setText(getArguments().getString("distancia", "Distancia no disponible"));
        }

        return view;
    }

    // Método para actualizar la distancia desde la actividad
    public void actualizarDistancia(String distancia) {
        if (textDistancia != null) {
            textDistancia.setText(distancia);
        }
    }
}
