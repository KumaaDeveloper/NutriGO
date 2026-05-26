package com.example.pedulimakanan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(RestoranModel store);
    }

    private Context context;
    private List<RestoranModel> stores;
    private OnItemClickListener listener;

    public RestaurantAdapter(Context context, List<RestoranModel> stores,
                             OnItemClickListener listener) {
        this.context  = context;
        this.stores   = stores;
        this.listener = listener;
    }

    public void updateData(List<RestoranModel> newStores) {
        this.stores = newStores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_restaurant_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RestoranModel store = stores.get(position);

        holder.tvNamaResto.setText(store.namaResto);
        holder.tvKategori.setText(store.kategori);

        // Map restaurant name → drawable resource
        int imageRes = getImageResource(store.namaResto);
        holder.imgResto.setImageResource(imageRes);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(store));
    }

    /**
     * Returns a drawable resource ID based on the restaurant name.
     * Add more entries here as you add more restaurants.
     * Falls back to the placeholder if no match is found.
     */
    private int getImageResource(String namaResto) {
        if (namaResto == null) return R.drawable.img_placeholder_food;
        switch (namaResto.toLowerCase().trim()) {
            case "saladstop":  return R.drawable.img_saladstop;
            case "burgreen":   return R.drawable.img_burgreen;
            case "supergrain": return R.drawable.img_supergrain;
            case "greenbowl":  return R.drawable.img_greenbowl;
            case "freshbox":   return R.drawable.img_freshbox;
            default:           return R.drawable.img_placeholder_food;
        }
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgResto;
        TextView  tvNamaResto, tvKategori;

        ViewHolder(View itemView) {
            super(itemView);
            imgResto    = itemView.findViewById(R.id.imgResto);
            tvNamaResto = itemView.findViewById(R.id.tvNamaResto);
            tvKategori  = itemView.findViewById(R.id.tvKategori);
        }
    }
}
