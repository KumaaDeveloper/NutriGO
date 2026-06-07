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
import java.util.Locale;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder> {

    private static final String PREF_NAME = "login_session";

    public interface OnItemClickListener {
        void onItemClick(RestoranModel store);
    }

    private Context context;
    private List<RestoranModel> stores;
    private OnItemClickListener listener;
    private DatabaseHelper db;
    private int userId;

    public RestaurantAdapter(Context context, List<RestoranModel> stores, OnItemClickListener listener) {
        this.context = context;
        this.stores = stores;
        this.listener = listener;

        db = new DatabaseHelper(context);

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
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

        holder.tvRatingResto.setText(String.format(Locale.US, "%.2f", store.rating));
        holder.tvTerjualResto.setText("Terjual " + store.terjual);

        int imageRes = getImageResource(store.namaResto);
        holder.imgResto.setImageResource(imageRes);

        holder.imgBtnFavorite.setImageResource(R.drawable.ic_favorite);

        boolean isFavorit = false;

        if (userId != -1 && store.id > 0) {
            isFavorit = db.isFavorit(userId, store.id);
        }

        setFavoriteIcon(holder.imgBtnFavorite, isFavorit);

        holder.btnFavoriteContainer.setOnClickListener(v -> {
            if (userId == -1) {
                Toast.makeText(context, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }

            if (store.id <= 0) {
                Toast.makeText(context, "Data restoran tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }

            db.toggleFavorit(userId, store.id);

            boolean nowFavorit = db.isFavorit(userId, store.id);
            setFavoriteIcon(holder.imgBtnFavorite, nowFavorit);

            if (nowFavorit) {
                Toast.makeText(
                        context,
                        store.namaResto + " ditambahkan ke favorit",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        context,
                        store.namaResto + " dihapus dari favorit",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(store);
            }
        });
    }

    private void setFavoriteIcon(ImageView imageView, boolean active) {
        imageView.setImageResource(R.drawable.ic_favorite);

        if (active) {
            imageView.setColorFilter(Color.parseColor("#FF4A4A"));
        } else {
            imageView.clearColorFilter();
        }
    }

    private int getImageResource(String namaResto) {
        if (namaResto == null) {
            return R.drawable.img_placeholder_food;
        }

        switch (namaResto.toLowerCase().trim()) {
            case "saladstop":
                return R.drawable.img_saladstop;

            case "burgreen":
                return R.drawable.img_burgreen;

            case "supergrain":
                return R.drawable.img_supergrain;

            case "greenbowl":
                return R.drawable.img_greenbowl;

            case "freshbox":
                return R.drawable.img_freshbox;

            case "nutrisnack":
                return R.drawable.img_nutri_snack;

            case "smoothiebar":
                return R.drawable.img_smoothie_bar;

            default:
                return R.drawable.img_placeholder_food;
        }
    }

    @Override
    public int getItemCount() {
        return stores == null ? 0 : stores.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgResto;
        ImageView imgBtnFavorite;
        TextView tvNamaResto;
        TextView tvKategori;
        TextView tvRatingResto;
        TextView tvTerjualResto;
        View btnFavoriteContainer;

        ViewHolder(View itemView) {
            super(itemView);

            imgResto = itemView.findViewById(R.id.imgResto);
            imgBtnFavorite = itemView.findViewById(R.id.imgBtnFavorite);
            tvNamaResto = itemView.findViewById(R.id.tvNamaResto);
            tvKategori = itemView.findViewById(R.id.tvKategori);
            tvRatingResto = itemView.findViewById(R.id.tvRatingResto);
            tvTerjualResto = itemView.findViewById(R.id.tvTerjualResto);
            btnFavoriteContainer = itemView.findViewById(R.id.btnFavoriteContainer);
        }
    }
}