package com.example.aitordas1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LugarAdapter extends RecyclerView.Adapter<LugarAdapter.LugarViewHolder> {
    private List<Lugar> listaLugares;
    private LugarDBManager dbManager;

    public LugarAdapter(Context context, List<Lugar> listaLugares) {
        this.listaLugares = listaLugares;
        this.dbManager = new LugarDBManager(context);
        dbManager.abrir();
    }

    @NonNull
    @Override
    public LugarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LugarViewHolder holder, int position) {
        Lugar lugar = listaLugares.get(position);
        holder.textNombre.setText(lugar.getNombre());
        holder.textDescripcion.setText(lugar.getDescripcion());

        // Botón para eliminar lugar
        holder.btnEliminar.setOnClickListener(v -> mostrarDialogoEliminar(v.getContext(), lugar, position));

        // Botón para editar lugar
        holder.btnEditar.setOnClickListener(v -> mostrarDialogoEditar(v.getContext(), lugar, position));

        // Clic normal para ver detalles del lugar
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetalleLugarActivity.class);
            intent.putExtra("nombre", lugar.getNombre());
            intent.putExtra("descripcion", lugar.getDescripcion());
            intent.putExtra("latitud", lugar.getLatitud()); // Añadido
            intent.putExtra("longitud", lugar.getLongitud()); // Añadido
            v.getContext().startActivity(intent);
        });


        // Pulsación larga para eliminar (opcional)
        holder.itemView.setOnLongClickListener(v -> {
            mostrarDialogoEliminar(v.getContext(), lugar, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaLugares.size();
    }

    static class LugarViewHolder extends RecyclerView.ViewHolder {
        TextView textNombre, textDescripcion;
        ImageButton btnEliminar, btnEditar;

        public LugarViewHolder(@NonNull View itemView) {
            super(itemView);
            textNombre = itemView.findViewById(R.id.textNombreLugar);
            textDescripcion = itemView.findViewById(R.id.textDescripcionLugar);
            btnEliminar = itemView.findViewById(R.id.btnEliminarLugar);
            btnEditar = itemView.findViewById(R.id.btnEditarLugar);
        }
    }

    // Método para mostrar el diálogo de confirmación antes de eliminar
    private void mostrarDialogoEliminar(Context context, Lugar lugar, int position) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_eliminar_titulo))
                .setMessage(String.format(context.getString(R.string.dialog_eliminar_mensaje), lugar.getNombre()))
                .setPositiveButton(context.getString(R.string.eliminar_lugar), (dialog, which) -> {
                    eliminarLugar(position);
                })
                .setNegativeButton(context.getString(R.string.cancelar), null)
                .show();
    }


    // Método para eliminar el lugar de la base de datos y la lista
    private void eliminarLugar(int position) {
        Lugar lugar = listaLugares.get(position);
        dbManager.eliminarLugar(lugar.getId()); // Elimina de la BD
        listaLugares.remove(position); // Elimina de la lista
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, listaLugares.size());
    }

    // Método para mostrar el diálogo de edición
    private void mostrarDialogoEditar(Context context, Lugar lugar, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.dialog_editar_titulo));

        final EditText inputNombre = new EditText(context);
        inputNombre.setText(lugar.getNombre());
        inputNombre.setHint(context.getString(R.string.nombre_lugar));
        inputNombre.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText inputDescripcion = new EditText(context);
        inputDescripcion.setText(lugar.getDescripcion());
        inputDescripcion.setHint(context.getString(R.string.descripcion_lugar));
        inputDescripcion.setInputType(InputType.TYPE_CLASS_TEXT);

        final EditText inputLatitud = new EditText(context);
        inputLatitud.setText(String.valueOf(lugar.getLatitud()));
        inputLatitud.setHint(context.getString(R.string.latitud_hint));
        inputLatitud.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);

        final EditText inputLongitud = new EditText(context);
        inputLongitud.setText(String.valueOf(lugar.getLongitud()));
        inputLongitud.setHint(context.getString(R.string.longitud_hint));
        inputLongitud.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(inputNombre);
        layout.addView(inputDescripcion);
        layout.addView(inputLatitud);
        layout.addView(inputLongitud);

        builder.setView(layout);

        builder.setPositiveButton(context.getString(R.string.guardar), (dialog, which) -> {
            String nuevoNombre = inputNombre.getText().toString().trim();
            String nuevaDescripcion = inputDescripcion.getText().toString().trim();
            String latitudTexto = inputLatitud.getText().toString().trim();
            String longitudTexto = inputLongitud.getText().toString().trim();

            if (!nuevoNombre.isEmpty() && !nuevaDescripcion.isEmpty() && !latitudTexto.isEmpty() && !longitudTexto.isEmpty()) {
                try {
                    double nuevaLatitud = Double.parseDouble(latitudTexto);
                    double nuevaLongitud = Double.parseDouble(longitudTexto);
                    actualizarLugar(lugar.getId(), nuevoNombre, nuevaDescripcion, nuevaLatitud, nuevaLongitud, position);
                } catch (NumberFormatException e) {
                    Toast.makeText(context, context.getString(R.string.error_lat_lon_invalidas), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, context.getString(R.string.error_campos_incompletos), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(context.getString(R.string.cancelar), (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void actualizarLugar(int id, String nuevoNombre, String nuevaDescripcion, double nuevaLatitud, double nuevaLongitud, int position) {
        dbManager.actualizarLugar(id, nuevoNombre, nuevaDescripcion, nuevaLatitud, nuevaLongitud);
        Lugar lugar = listaLugares.get(position);
        lugar.setNombre(nuevoNombre);
        lugar.setDescripcion(nuevaDescripcion);
        lugar.setLatitud(nuevaLatitud);
        lugar.setLongitud(nuevaLongitud);
        notifyItemChanged(position);
    }


}
