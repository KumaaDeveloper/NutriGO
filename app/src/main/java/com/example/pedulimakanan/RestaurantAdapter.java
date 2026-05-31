package com.example.pedulimakanan;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private DatabaseHelper db;
    private int userId;

    public RestaurantAdapter(Context context, List<RestoranModel> stores,
                             OnItemClickListener listener) {
        this.context  = context;
        this.stores   = stores;
        this.listener = listener;

        this.db = new DatabaseHelper(context);
        SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        this.userId = prefs.getInt("user_id", -1);
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

        int imageRes = getImageResource(store.namaResto);
        holder.imgResto.setImageResource(imageRes);

        // ── KONTROL STATE WARNA TOGGLE FAVORIT (IC_FAVORITE TUNGGAL) ──
        if (userId != -1 && store.id != 0) {
            if (db.isFavorit(userId, store.id)) {
                holder.imgBtnFavorite.setColorFilter(Color.parseColor("#FF4A4A")); // Filter Merah
            } else {
                holder.imgBtnFavorite.setColorFilter(Color.parseColor("#FFFFFF")); // Filter Putih (Outline Hitam bawaan XML tetap aman)
            }

            holder.imgBtnFavorite.setOnClickListener(v -> {
                db.toggleFavorit(userId, store.id);

                if (db.isFavorit(userId, store.id)) {
                    holder.imgBtnFavorite.setColorFilter(Color.parseColor("#FF4A4A"));
                    Toast.makeText(context, store.namaResto + " ditambah ke favorit", Toast.LENGTH_SHORT).show();
                } else {
                    holder.imgBtnFavorite.setColorFilter(Color.parseColor("#FFFFFF"));
                    Toast.makeText(context, store.namaResto + " dihapus dari favorit", Toast.LENGTH_SHORT).show();
                }
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(store));
    }

    private int getImageResource(String namaResto) {
        if (namaResto == null) return R.drawable.img_placeholder_food;
        switch (namaResto.toLowerCase().trim()) {
            case "saladstop":  return R.drawable.img_saladstop;
            case "burgreen":   return R.drawable.img_burgreen;
            case "supergrain": return R.drawable.img_supergrain;
            case "greenbowl":  return R.drawable.img_greenbowl;
            case "freshbox":   return R.drawable.img_freshbox;
            case "nutrisnack": return R.drawable.img_nutri_snack;
            case "smoothiebar": return R.drawable.img_smoothie_bar;
            default:           return R.drawable.img_placeholder_food;
        }
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgResto, imgBtnFavorite;
        TextView  tvNamaResto, tvKategori;

        ViewHolder(View itemView) {
            super(itemView);
            imgResto        = itemView.findViewById(R.id.imgResto);
            imgBtnFavorite  = itemView.findViewById(R.id.imgBtnFavorite);
            tvNamaResto     = itemView.findViewById(R.id.tvNamaResto);
            tvKategori      = itemView.findViewById(R.id.tvKategori);
        }
    }
}