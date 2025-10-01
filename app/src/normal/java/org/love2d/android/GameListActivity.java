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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GameListActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "GameHubPrefs";
    private static final String GAMES_FOLDER_URI = "games_folder_uri";

    private final Executor executor = Executors.newSingleThreadExecutor();

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeLayout;
    private ConstraintLayout noGameLayout;
    private EditText searchEditText;
    private FloatingActionButton changeFolderFab;
    private GameGridAdapter adapter;
    private String folderUri;

    private final ActivityResultLauncher<String[]> openFileLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        (Uri result) -> {
            if (result != null) {
                Intent intent = new Intent(this, GameActivity.class);
                intent.setData(result);
                startActivity(intent);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_game_list);
        EdgeToEdgeHelper.applySystemBarsPadding(findViewById(R.id.gameListRoot));

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        setupSearch();

        // Get folder URI from intent or preferences
        folderUri = getIntent().getStringExtra("folder_uri");
        if (folderUri == null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            folderUri = prefs.getString(GAMES_FOLDER_URI, null);
        }

        if (folderUri != null) {
            scanGames();
        } else {
            // No folder selected, redirect to folder selector
            startActivity(new Intent(this, FolderSelectorActivity.class));
            finish();
        }
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.gamesRecyclerView);
        swipeLayout = findViewById(R.id.swipeRefreshLayout);
        noGameLayout = findViewById(R.id.noGamesLayout);
        searchEditText = findViewById(R.id.searchEditText);
        changeFolderFab = findViewById(R.id.changeFolderFab);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("GameHub");
        }
    }

    private void setupRecyclerView() {
        adapter = new GameGridAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeLayout.setOnRefreshListener(this::scanGames);
        
        changeFolderFab.setOnClickListener(v -> {
            startActivity(new Intent(this, FolderSelectorActivity.class));
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_open_file) {
            openFileLauncher.launch(new String[]{"*/*"});
            return true;
        } else if (itemId == R.id.action_launch_nogame) {
            Intent intent = new Intent(this, GameActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void scanGames() {
        if (folderUri == null) {
            runOnUiThread(() -> {
                swipeLayout.setRefreshing(false);
                noGameLayout.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Nenhuma pasta selecionada", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        executor.execute(() -> {
            try {
                Uri uri = Uri.parse(folderUri);
                DocumentFile folder = DocumentFile.fromTreeUri(this, uri);
                
                if (folder != null && folder.exists() && folder.canRead()) {
                    ArrayList<GameGridAdapter.GameData> validGames = new ArrayList<>();
                    DocumentFile[] files = folder.listFiles();

                    if (files != null && files.length > 0) {
                        for (DocumentFile file : files) {
                            if (file == null || !file.exists()) continue;
                            
                            GameGridAdapter.GameData gameData = null;

                            if (file.isDirectory()) {
                                if (isValidGameDirectory(file)) {
                                    gameData = new GameGridAdapter.GameData();
                                    gameData.documentFile = file;
                                    gameData.name = file.getName() != null ? file.getName() : "Jogo sem nome";
                                    gameData.isDirectory = true;
                                }
                            } else if (file.getName() != null && file.getName().toLowerCase().endsWith(".love")) {
                                gameData = new GameGridAdapter.GameData();
                                gameData.documentFile = file;
                                String fileName = file.getName();
                                gameData.name = fileName.substring(0, fileName.lastIndexOf('.'));
                                gameData.isDirectory = false;
                            }

                            if (gameData != null) {
                                validGames.add(gameData);
                            }
                        }
                    }

                    final boolean empty = validGames.isEmpty();
                    final GameGridAdapter.GameData[] gameArray = empty ? null : validGames.toArray(new GameGridAdapter.GameData[0]);

                    runOnUiThread(() -> {
                        try {
                            adapter.setData(gameArray);
                            adapter.notifyDataSetChanged();
                            swipeLayout.setRefreshing(false);
                            noGameLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
                            
                            if (!empty) {
                                Toast.makeText(this, validGames.size() + " jogos encontrados", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Erro ao exibir jogos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        swipeLayout.setRefreshing(false);
                        noGameLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Pasta não acessível. Tente selecionar uma pasta diferente.", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (SecurityException e) {
                runOnUiThread(() -> {
                    swipeLayout.setRefreshing(false);
                    noGameLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Permissão negada para acessar a pasta", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    swipeLayout.setRefreshing(false);
                    noGameLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Erro inesperado: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private boolean isValidGameDirectory(DocumentFile directory) {
        try {
            if (directory == null || !directory.exists() || !directory.isDirectory()) {
                return false;
            }
            
            DocumentFile[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                return false;
            }
            
            for (DocumentFile file : files) {
                if (file != null && file.exists() && !file.isDirectory() && "main.lua".equals(file.getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore errors and return false
        }
        return false;
    }
}