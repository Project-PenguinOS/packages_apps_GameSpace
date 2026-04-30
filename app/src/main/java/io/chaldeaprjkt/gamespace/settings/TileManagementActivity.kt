/*
 * Copyright (C) 2026 kenway214
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.chaldeaprjkt.gamespace.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.gamebar.tiles.TileAction
import io.chaldeaprjkt.gamespace.gamebar.tiles.TileRepository
import javax.inject.Inject

@AndroidEntryPoint(ComponentActivity::class)
class TileManagementActivity : Hilt_TileManagementActivity() {

    @Inject
    lateinit var tileRepository: TileRepository

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tileRepository.ensureInitialized()

        setContent {
            val themeColorScheme = dynamicDarkColorScheme(this)
            MaterialTheme(colorScheme = themeColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TileManagementScreen(
                        tileRepository = tileRepository,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileManagementScreen(
    tileRepository: TileRepository,
    onBack: () -> Unit
) {
    val allTiles = remember { tileRepository.allAvailableTiles }
    val activeTiles = remember { mutableStateListOf<TileAction>().apply { addAll(tileRepository.tiles) } }
    val availableTiles = remember(activeTiles.size) {
        val activeIds = activeTiles.map { it.id }.toSet()
        allTiles.filter { it.id !in activeIds }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.game_tools_tiles_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.selected_tiles),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (activeTiles.isEmpty()) {
                item {
                    EmptyTileState(stringResource(R.string.drag_tiles_here))
                }
            } else {
                items(activeTiles, key = { it.id }) { tile ->
                    TileManagerCard(
                        tile = tile,
                        isActive = true,
                        onAction = {
                            activeTiles.remove(tile)
                            tileRepository.updateTileSelection(activeTiles.map { it.id })
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.available_tiles),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (availableTiles.isEmpty()) {
                item {
                    EmptyTileState(stringResource(R.string.all_tiles_added))
                }
            } else {
                items(availableTiles, key = { it.id }) { tile ->
                    TileManagerCard(
                        tile = tile,
                        isActive = false,
                        onAction = {
                            activeTiles.add(tile)
                            tileRepository.updateTileSelection(activeTiles.map { it.id })
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TileManagerCard(
    tile: TileAction,
    isActive: Boolean,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(tile.icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onAction,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isActive) 
                        MaterialTheme.colorScheme.errorContainer
                    else 
                        MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isActive)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Rounded.Remove else Icons.Rounded.Add,
                    contentDescription = null
                )
            }
            
            if (isActive) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun EmptyTileState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
