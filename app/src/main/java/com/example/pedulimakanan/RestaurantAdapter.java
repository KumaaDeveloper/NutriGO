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
        this.context = context; // Memastikan objek context terikat dengan benar
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant_card,
                parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RestoranModel store = stores.get(position);
        holder.tvNamaResto.setText(store.namaResto);
        holder.tvRating.setText(String.format("%.1f", store.rating));
        holder.tvKategori.setText(store.kategori);

        if (store.gambarUrl != null && !store.gambarUrl.isEmpty()) {

            // JIKA URL INTERNET: Jalankan AsyncTask bawaan awal kamu (1 Argumen)
            if (store.gambarUrl.startsWith("http://") || store.gambarUrl.startsWith("https://")) {
                new LoadImageTask(holder.imgResto).execute(store.gambarUrl);
            }

            // JIKA LOKAL SQLITE: Ambil via Context langsung dari parent view agar anti-null
            else {
                Context viewContext = holder.itemView.getContext();
                System.out.println("LOG_NUTRI_GO -> Nama Resto: " + store.namaResto + " | String Gambar di DB: '" + store.gambarUrl + "'");
                int resId = viewContext.getResources().getIdentifier(
                        store.gambarUrl,
                        "drawable",
                        viewContext.getPackageName()
                );

                System.out.println("LOG_NUTRI_GO -> Hasil ID Drawable: " + resId);

                if (resId != 0) {
                    holder.imgResto.setImageResource(resId);
                } else {
                    holder.imgResto.setImageResource(R.drawable.img_placeholder_food);
                }
            }
        } else {
            holder.imgResto.setImageResource(R.drawable.img_placeholder_food);
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

    // Melanjutkan LoadImageTask bentuk awal bawaan kamu (Murni hanya urusan URL)
    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String pathGambar = urls[0];
            if (pathGambar.startsWith("http://") || pathGambar.startsWith("https://")) {
                try {
                    URL url = new URL(pathGambar);
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
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}