package com.example.pedulimakanan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
        this.context = context;
        this.stores = stores;
        this.listener = listener;
    }

    public void updateData(List<RestoranModel> newStores) {
        this.stores = newStores;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_restaurant_card,
                parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RestoranModel store = stores.get(position);
        holder.tvNamaResto.setText(store.namaResto);
        holder.tvRating.setText(String.format("%.1f", store.rating));
        holder.tvKategori.setText(store.kategori);

        // Load image
        if (store.gambarUrl != null && !store.gambarUrl.isEmpty()) {
            new LoadImageTask(holder.imgResto).execute(store.gambarUrl);
        } else {
            holder.imgResto.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(store));
    }

    @Override
    public int getItemCount() {
        return stores.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgResto;
        TextView tvNamaResto, tvRating, tvKategori;

        ViewHolder(View itemView) {
            super(itemView);
            imgResto = itemView.findViewById(R.id.imgResto);
            tvNamaResto = itemView.findViewById(R.id.tvNamaResto);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvKategori = itemView.findViewById(R.id.tvKategori);
        }
    }

    // Simple async image loader (no Glide needed since you have no Glide dependency)
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();
                InputStream is = conn.getInputStream();
                return BitmapFactory.decodeStream(is);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}