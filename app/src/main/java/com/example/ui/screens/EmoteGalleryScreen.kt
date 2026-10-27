package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.EmoteCategory
import com.example.data.EmoteGalleryCatalog
import com.example.data.EmoteMomentTrigger
import com.example.data.PawnEmoteItem
import com.example.data.PlayerColor
import com.example.data.PlayerProfile
import com.example.ui.board3d.PawnEmote3DPreviewStage
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrassHighlight
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.Obsidianink
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmoteGalleryScreen(
  uiState: LudoUiState,
  profileState: DataStreamState<PlayerProfile?>,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val profile = (profileState as? DataStreamState.Success)?.data
  val currentCoins = if (viewModel.isAuthenticated && profile != null) {
    profile.coins
  } else {
    uiState.localGuestCoins
  }
  val currentXp = if (viewModel.isAuthenticated && profile != null) {
    profile.xp
  } else {
    uiState.localGuestXp
  }

  var previewPawnColor by remember { mutableStateOf(uiState.preferredOnlineColor) }
  val previewEmote = uiState.previewEmote
  val selectedMomentSlot = uiState.selectedEmoteMomentSlot
  val isPreviewUnlocked = viewModel.isEmoteUnlocked(previewEmote, profile)
  val isPreviewEquippedToActiveSlot = uiState.equippedEmoteForMoment(selectedMomentSlot).id == previewEmote.id

  val filteredEmotes = remember(uiState.selectedEmoteCategoryFilter) {
    val filter = uiState.selectedEmoteCategoryFilter
    if (filter == null) {
      EmoteGalleryCatalog.ALL_EMOTES
    } else {
      EmoteGalleryCatalog.ALL_EMOTES.filter { it.category == filter }
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Top Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = { viewModel.leaveToLobby() },
              modifier = Modifier.testTag("emote_gallery_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Lobby"
              )
            }
            Column {
              Text(
                text = "3D Emote & Sticker Gallery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Unlock & equip 3D animations and stickers above your pawns",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, BrushedBrass, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("emote_gallery_coins_pill"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Royal Coins",
                tint = BrushedBrass,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "$currentCoins",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            IconButton(
              onClick = { viewModel.openStoreScreen() },
              modifier = Modifier
                .clip(CircleShape)
                .background(BrushedBrass.copy(alpha = 0.16f))
                .testTag("emote_gallery_open_store_button")
            ) {
              Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "Get Coins in Store",
                tint = BrushedBrass
              )
            }
          }
        }

        // Toast or Error Banner
        if (!uiState.toastMessage.isNullOrBlank() || !uiState.errorMessage.isNullOrBlank()) {
          val isErr = !uiState.errorMessage.isNullOrBlank()
          val msg = uiState.errorMessage ?: uiState.toastMessage.orEmpty()
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(
                1.dp,
                if (isErr) TerracottaRed else EucalyptusSage,
                RoundedCornerShape(14.dp)
              ),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = if (isErr) TerracottaRed.copy(alpha = 0.12f) else EucalyptusSage.copy(alpha = 0.14f)
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = msg,
                style = MaterialTheme.typography.labelLarge,
                color = if (isErr) TerracottaRed else EucalyptusSage,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = "Dismiss",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                  .clickable { viewModel.clearErrorMessage() }
                  .padding(start = 8.dp)
              )
            }
          }
        }

        // Live 3D Pawn Emote & Sticker Preview Stage Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BrushedBrass, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ViewInAr,
                  contentDescription = null,
                  tint = BrushedBrass,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "LIVE 3D PAWN SHOWCASE",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(BrushedBrass.copy(alpha = 0.16f))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "${previewEmote.rarityLabel} • ${previewEmote.category.label}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
              }
            }

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Obsidianink.copy(alpha = 0.06f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
              PawnEmote3DPreviewStage(
                emote = previewEmote,
                pawnColor = previewPawnColor,
                triggerMomentLabel = selectedMomentSlot.label,
                modifier = Modifier.fillMaxSize()
              )
            }

            // Preview Pawn Color Swatch Strip
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Preview Pawn Color:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlayerColor.entries.forEach { color ->
                  val (primary, _, _) = paletteForColor(color)
                  val isSelected = color == previewPawnColor
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(primary)
                      .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) BrushedBrass else Color.White.copy(alpha = 0.6f),
                        shape = CircleShape
                      )
                      .clickable { previewPawnColor = color }
                      .testTag("emote_preview_color_${color.name}"),
                    contentAlignment = Alignment.Center
                  ) {
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = color.label,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                      )
                    }
                  }
                }
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Selected Emote Details & Action Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${previewEmote.stickerSymbol} ${previewEmote.title}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = previewEmote.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Target Moment Slot: ${selectedMomentSlot.label}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = BrushedBrass
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Button(
                onClick = {
                  viewModel.unlockOrEquipPawnEmote(
                    emote = previewEmote,
                    targetMoment = selectedMomentSlot,
                    profile = profile
                  )
                },
                enabled = !isPreviewEquippedToActiveSlot,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isPreviewUnlocked) EucalyptusSage else BrushedBrass,
                  contentColor = Color.White
                ),
                modifier = Modifier
                  .height(48.dp)
                  .testTag("preview_unlock_or_equip_button")
              ) {
                if (isPreviewEquippedToActiveSlot) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Equipped", fontWeight = FontWeight.Bold)
                } else if (isPreviewUnlocked) {
                  Text("Equip to Slot", fontWeight = FontWeight.Bold)
                } else {
                  Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Unlock (${previewEmote.coinCost})", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // Special Game Moment Loadout Slots (5 Triggers)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Special Game Moment Loadout",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Tap a game moment slot below to assign which 3D animation or sticker triggers automatically",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              EmoteMomentTrigger.entries.forEach { moment ->
                val equipped = uiState.equippedEmoteForMoment(moment)
                val isSlotSelected = moment == selectedMomentSlot
                Card(
                  modifier = Modifier
                    .width(156.dp)
                    .border(
                      width = if (isSlotSelected) 2.dp else 1.dp,
                      color = if (isSlotSelected) BrushedBrass else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { viewModel.selectEmoteMomentSlot(moment) }
                    .testTag("moment_slot_${moment.id}"),
                  shape = RoundedCornerShape(14.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isSlotSelected) {
                      BrushedBrass.copy(alpha = 0.14f)
                    } else {
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    }
                  )
                ) {
                  Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Text(
                      text = moment.shortTag,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isSlotSelected) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = equipped.stickerSymbol,
                        style = MaterialTheme.typography.titleLarge
                      )
                      Text(
                        text = equipped.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                    Text(
                      text = equipped.category.label,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }

        // Category Filter Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = uiState.selectedEmoteCategoryFilter == null,
            onClick = { viewModel.setEmoteCategoryFilter(null) },
            label = { Text("All (${EmoteGalleryCatalog.ALL_EMOTES.size})") },
            modifier = Modifier.testTag("filter_emotes_all")
          )
          FilterChip(
            selected = uiState.selectedEmoteCategoryFilter == EmoteCategory.ANIMATION_3D,
            onClick = { viewModel.setEmoteCategoryFilter(EmoteCategory.ANIMATION_3D) },
            label = { Text("3D Animations") },
            modifier = Modifier.testTag("filter_emotes_3d")
          )
          FilterChip(
            selected = uiState.selectedEmoteCategoryFilter == EmoteCategory.VISUAL_STICKER,
            onClick = { viewModel.setEmoteCategoryFilter(EmoteCategory.VISUAL_STICKER) },
            label = { Text("Visual Stickers") },
            modifier = Modifier.testTag("filter_emotes_stickers")
          )
        }

        // Emote & Sticker Collection Cards
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          filteredEmotes.forEach { emote ->
            val isUnlocked = viewModel.isEmoteUnlocked(emote, profile)
            val isPreviewSelected = emote.id == previewEmote.id
            val equippedMoments = EmoteMomentTrigger.entries.filter {
              uiState.equippedEmoteForMoment(it).id == emote.id
            }

            EmoteGalleryItemCard(
              emote = emote,
              isUnlocked = isUnlocked,
              isPreviewSelected = isPreviewSelected,
              equippedMoments = equippedMoments,
              activeTargetMoment = selectedMomentSlot,
              onSelectPreview = { viewModel.selectEmoteForPreview(emote.id) },
              onUnlockOrEquip = {
                viewModel.unlockOrEquipPawnEmote(
                  emote = emote,
                  targetMoment = selectedMomentSlot,
                  profile = profile
                )
              }
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmoteGalleryItemCard(
  emote: PawnEmoteItem,
  isUnlocked: Boolean,
  isPreviewSelected: Boolean,
  equippedMoments: List<EmoteMomentTrigger>,
  activeTargetMoment: EmoteMomentTrigger,
  onSelectPreview: () -> Unit,
  onUnlockOrEquip: () -> Unit
) {
  val isEquippedInActiveSlot = activeTargetMoment in equippedMoments

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isPreviewSelected) 2.dp else 1.dp,
        color = when {
          isPreviewSelected -> BrushedBrass
          equippedMoments.isNotEmpty() -> EucalyptusSage.copy(alpha = 0.7f)
          else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(18.dp)
      )
      .clickable { onSelectPreview() }
      .testTag("emote_card_${emote.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isPreviewSelected) {
        BrushedBrass.copy(alpha = 0.08f)
      } else {
        MaterialTheme.colorScheme.surface
      }
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(50.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(Obsidianink.copy(alpha = 0.90f))
              .border(1.5.dp, BrushedBrass, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = emote.stickerSymbol,
              style = MaterialTheme.typography.headlineSmall
            )
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = emote.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(BrushedBrass.copy(alpha = 0.18f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = emote.rarityLabel,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
              }
            }

            Text(
              text = emote.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
              text = "${emote.category.label} • Callout: \"${emote.calloutText}\"",
              style = MaterialTheme.typography.labelSmall,
              color = BrushedBrass
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        if (isEquippedInActiveSlot) {
          OutlinedButton(
            onClick = onSelectPreview,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("emote_action_${emote.id}")
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = EucalyptusSage,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Equipped", color = EucalyptusSage, fontWeight = FontWeight.Bold)
          }
        } else if (isUnlocked) {
          Button(
            onClick = onUnlockOrEquip,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = EucalyptusSage,
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("emote_action_${emote.id}")
          ) {
            Text("Equip", fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = onUnlockOrEquip,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = BrushedBrass,
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("emote_action_${emote.id}")
          ) {
            Icon(
              imageVector = Icons.Default.MonetizationOn,
              contentDescription = null,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("${emote.coinCost}", fontWeight = FontWeight.Bold)
          }
        }
      }

      if (equippedMoments.isNotEmpty()) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          equippedMoments.forEach { moment ->
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(EucalyptusSage.copy(alpha = 0.14f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = EucalyptusSage,
                modifier = Modifier.size(12.dp)
              )
              Text(
                text = "Active on ${moment.label}",
                style = MaterialTheme.typography.labelSmall,
                color = EucalyptusSage,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
    }
  }
}
