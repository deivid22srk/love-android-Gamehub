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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GameGridAdapter extends RecyclerView.Adapter<GameGridAdapter.ViewHolder> {

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
        Color.parseColor("#667db6"),
    };

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
        if (allData == null) return;

        filteredData.clear();
        if (query.isEmpty()) {
            filteredData.addAll(Arrays.asList(allData));
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (GameData game : allData) {
                if (game.name != null && game.name.toLowerCase().contains(lowerCaseQuery)) {
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
        private DocumentFile documentFile;

        public ViewHolder(View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.gameCard);
            gameIcon = itemView.findViewById(R.id.gameIcon);
            gameName = itemView.findViewById(R.id.gameName);
            gradientBackground = itemView.findViewById(R.id.gradientBackground);
        }

        public void setData(GameData data, int position) {
            gameName.setText(data.name);
            documentFile = data.documentFile;

            // Set icon based on type
            if (data.isDirectory) {
                gameIcon.setImageResource(R.drawable.ic_baseline_folder_32);
            } else {
                gameIcon.setImageResource(R.drawable.ic_baseline_insert_drive_file_32);
            }

            // Create gradient background
            createGradientBackground(position);
        }

        private void createGradientBackground(int position) {
            // Use position to ensure consistent colors for the same game
            int colorIndex1 = position % gradientColors.length;
            int colorIndex2 = (position + 1) % gradientColors.length;
            
            int color1 = gradientColors[colorIndex1];
            int color2 = gradientColors[colorIndex2];
            
            // Make colors more muted for better readability
            color1 = adjustColorAlpha(color1, 0.8f);
            color2 = adjustColorAlpha(color2, 0.8f);

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
            if (documentFile == null) {
                return;
            }

            Context context = v.getContext();
            Intent intent = new Intent(context, GameActivity.class);
            intent.setData(documentFile.getUri());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        }
    }

    static class GameData {
        public DocumentFile documentFile;
        public String name;
        public boolean isDirectory;
    }
}