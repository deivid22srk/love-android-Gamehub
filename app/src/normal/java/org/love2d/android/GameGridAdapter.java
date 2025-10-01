/*
 * Copyright (c) 2006-2024 LOVE Development Team
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package org.love2d.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GameGridAdapter extends RecyclerView.Adapter<GameGridAdapter.ViewHolder> {
    private static final String[] ICON_PRIORITY = {
        "icon.png",
        "icon.jpg",
        "icon.jpeg",
        "icon.webp",
        "logo.png",
        "logo.jpg",
        "logo.jpeg",
        "logo.webp"
    };

    private final Context context;
    private final ExecutorService iconExecutor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Bitmap> iconCache = new ConcurrentHashMap<>();
    private GameData[] allData = null;
    private List<GameData> filteredData = new ArrayList<>();
    private final int[] gradientColors = {
        Color.parseColor("#667eea"),
        Color.parseColor("#764ba2"),
        Color.parseColor("#f093fb"),
        Color.parseColor("#f5576c"),
        Color.parseColor("#4facfe"),
        Color.parseColor("#00f2fe"),
        Color.parseColor("#43e97b"),
        Color.parseColor("#38f9d7"),
        Color.parseColor("#ffecd2"),
        Color.parseColor("#fcb69f"),
        Color.parseColor("#a8edea"),
        Color.parseColor("#fed6e3"),
        Color.parseColor("#ffd89b"),
        Color.parseColor("#19547b"),
        Color.parseColor("#667db6")
    };

    public GameGridAdapter(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_grid, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(filteredData.get(position), position);
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    public void setData(GameData[] data) {
        this.allData = data;
        if (data == null) {
            filteredData.clear();
        } else {
            filteredData = new ArrayList<>(Arrays.asList(data));
        }
    }

    public void filter(String query) {
        if (allData == null) {
            return;
        }
        filteredData.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredData.addAll(Arrays.asList(allData));
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.ROOT).trim();
            for (GameData game : allData) {
                if (game != null && game.name != null && game.name.toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                    filteredData.add(game);
                }
            }
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final MaterialCardView cardView;
        private final ImageView gameIcon;
        private final TextView gameName;
        private final View gradientBackground;
        private GameData data;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.gameCard);
            gameIcon = itemView.findViewById(R.id.gameIcon);
            gameName = itemView.findViewById(R.id.gameName);
            gradientBackground = itemView.findViewById(R.id.gradientBackground);
        }

        void setData(GameData data, int position) {
            this.data = data;
            gameName.setText(data.name);
            createGradientBackground(position);
            loadIcon(this, data);
        }

        private void createGradientBackground(int position) {
            int colorIndex1 = position % gradientColors.length;
            int colorIndex2 = (position + 1) % gradientColors.length;
            int color1 = adjustColorAlpha(gradientColors[colorIndex1], 0.8f);
            int color2 = adjustColorAlpha(gradientColors[colorIndex2], 0.8f);
            GradientDrawable gradient = new GradientDrawable();
            gradient.setShape(GradientDrawable.RECTANGLE);
            gradient.setCornerRadius(24f);
            gradient.setColors(new int[]{color1, color2});
            gradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            gradient.setOrientation(GradientDrawable.Orientation.TL_BR);
            gradientBackground.setBackground(gradient);
        }

        private int adjustColorAlpha(int color, float alpha) {
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            return Color.argb((int) (255 * alpha), red, green, blue);
        }

        @Override
        public void onClick(View v) {
            if (data == null || data.documentFile == null || !data.documentFile.exists()) {
                Toast.makeText(v.getContext(), "Arquivo do jogo não encontrado", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Context context = v.getContext();
                Intent intent = new Intent(context, GameActivity.class);
                intent.setData(data.documentFile.getUri());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Erro ao abrir jogo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadIcon(ViewHolder holder, GameData data) {
        String key = data.documentFile != null ? data.documentFile.getUri().toString() : data.name;
        Bitmap cached = key != null ? iconCache.get(key) : null;
        if (cached != null) {
            holder.gameIcon.setImageBitmap(cached);
            return;
        }
        holder.gameIcon.setImageResource(data.isDirectory ? R.drawable.ic_baseline_folder_32 : R.drawable.ic_baseline_insert_drive_file_32);
        iconExecutor.execute(() -> {
            Bitmap bitmap = fetchIcon(data);
            if (bitmap == null || key == null) {
                return;
            }
            iconCache.put(key, bitmap);
            mainHandler.post(() -> {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= filteredData.size()) {
                    return;
                }
                GameData current = filteredData.get(position);
                if (current == data) {
                    holder.gameIcon.setImageBitmap(bitmap);
                }
            });
        });
    }

    private Bitmap fetchIcon(GameData data) {
        DocumentFile file = data.documentFile;
        if (file == null) {
            return null;
        }
        try {
            if (data.isDirectory) {
                return loadIconFromDirectory(file, data.name);
            }
            return loadIconFromArchive(file, data.name);
        } catch (IOException ignored) {
        }
        return null;
    }

    private Bitmap loadIconFromDirectory(DocumentFile directory, String gameName) throws IOException {
        DocumentFile[] children = directory.listFiles();
        if (children == null || children.length == 0) {
            return null;
        }
        DocumentFile target = findMatchingFile(children, gameName);
        if (target == null) {
            return null;
        }
        try (InputStream stream = context.getContentResolver().openInputStream(target.getUri())) {
            if (stream == null) {
                return null;
            }
            byte[] bytes = readAllBytes(stream);
            return decodeBitmap(bytes);
        }
    }

    private Bitmap loadIconFromArchive(DocumentFile file, String gameName) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(file.getUri())) {
            if (inputStream == null) {
                return null;
            }
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        zipInputStream.closeEntry();
                        continue;
                    }
                    String name = entry.getName();
                    if (name == null) {
                        zipInputStream.closeEntry();
                        continue;
                    }
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (matchesIconName(lower, gameName)) {
                        byte[] bytes = readAllBytes(zipInputStream);
                        Bitmap bitmap = decodeBitmap(bytes);
                        zipInputStream.closeEntry();
                        if (bitmap != null) {
                            return bitmap;
                        }
                    } else {
                        zipInputStream.closeEntry();
                    }
                }
            }
        }
        return null;
    }

    private DocumentFile findMatchingFile(DocumentFile[] files, String gameName) {
        for (String name : ICON_PRIORITY) {
            DocumentFile file = findByExactName(files, name);
            if (file != null) {
                return file;
            }
        }
        if (gameName != null) {
            String base = gameName.toLowerCase(Locale.ROOT);
            String[] extensions = {".png", ".jpg", ".jpeg", ".webp"};
            for (String extension : extensions) {
                DocumentFile file = findByExactName(files, base + extension);
                if (file != null) {
                    return file;
                }
            }
        }
        return null;
    }

    private DocumentFile findByExactName(DocumentFile[] files, String name) {
        for (DocumentFile file : files) {
            if (file == null || file.isDirectory()) {
                continue;
            }
            String fileName = file.getName();
            if (fileName == null) {
                continue;
            }
            if (fileName.toLowerCase(Locale.ROOT).equals(name)) {
                return file;
            }
        }
        return null;
    }

    private boolean matchesIconName(String entryName, String gameName) {
        for (String name : ICON_PRIORITY) {
            if (entryName.endsWith(name)) {
                return true;
            }
        }
        if (gameName != null) {
            String base = gameName.toLowerCase(Locale.ROOT);
            if (entryName.endsWith(base + ".png") || entryName.endsWith(base + ".jpg") || entryName.endsWith(base + ".jpeg") || entryName.endsWith(base + ".webp")) {
                return true;
            }
        }
        return false;
    }

    private byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = stream.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    private Bitmap decodeBitmap(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        options.inSampleSize = calculateInSampleSize(options, 256, 256);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (bitmap == null) {
            return null;
        }
        if (bitmap.getWidth() > 256 || bitmap.getHeight() > 256) {
            float ratio = Math.min(256f / bitmap.getWidth(), 256f / bitmap.getHeight());
            int targetWidth = Math.max(1, Math.round(bitmap.getWidth() * ratio));
            int targetHeight = Math.max(1, Math.round(bitmap.getHeight() * ratio));
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            if (scaled != bitmap) {
                bitmap.recycle();
                bitmap = scaled;
            }
        }
        return bitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    static class GameData {
        public DocumentFile documentFile;
        public String name;
        public boolean isDirectory;
    }
}
