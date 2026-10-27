package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AdminAppConfig
import com.example.data.CountryCatalog
import com.example.data.PlayerProfile
import com.example.data.RoyalStoreCatalog
import com.example.data.StoreItem
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

@Composable
fun RoyalBoutiqueStoreScreen(
  uiState: LudoUiState,
  profileState: DataStreamState<PlayerProfile?>,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val profile = (profileState as? DataStreamState.Success)?.data
  val activeCountryCode = profile?.countryCode ?: uiState.selectedCountryFilter
  val countryInfo = CountryCatalog.fromCode(activeCountryCode)

  val coinsBalance = if (viewModel.isAuthenticated && profile != null) {
    profile.coins
  } else {
    uiState.localGuestCoins
  }
  val totalXp = if (viewModel.isAuthenticated && profile != null) {
    profile.xp
  } else {
    uiState.localGuestXp
  }
  val playerLevel = ((totalXp / 200L) + 1L).toInt().coerceAtLeast(1)
  val xpIntoLevel = (totalXp % 200L).toInt()
  val unlockedIds = ((profile?.unlockedItems.orEmpty()) + uiState.localUnlockedItems).distinct()

  var pendingIapItem by remember { mutableStateOf<StoreItem?>(null) }
  var showLocationCurrencyPicker by remember { mutableStateOf(false) }

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
          .widthIn(max = 600.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = { viewModel.leaveToLobby() },
              modifier = Modifier.testTag("store_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Lobby"
              )
            }
            Column {
              Text(
                text = "Royal Boutique & Store",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "${countryInfo.flagEmoji} ${countryInfo.name} • Prices in ${countryInfo.currencyCode} (${countryInfo.currencySymbol.trim()})",
                style = MaterialTheme.typography.labelMedium,
                color = BrushedBrass
              )
            }
          }

          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(BrushedBrass.copy(alpha = 0.16f))
              .border(1.dp, BrushedBrass, RoundedCornerShape(16.dp))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Icon(
                imageVector = Icons.Default.MonetizationOn,
                contentDescription = "Royal Coins",
                tint = BrushedBrass,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "$coinsBalance Coins",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = "≈ ${CountryCatalog.formatCoinsEquivalent(coinsBalance, activeCountryCode)}",
              style = MaterialTheme.typography.labelSmall,
              color = BrushedBrass,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        // Location & Regional Currency Selector Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrushedBrass.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(
            containerColor = BrushedBrass.copy(alpha = 0.08f)
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = BrushedBrass
              )
              Column {
                Text(
                  text = "Location: ${countryInfo.flagEmoji} ${countryInfo.name}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Active Currency: ${countryInfo.currencyCode} (${countryInfo.currencySymbol.trim()})",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            OutlinedButton(
              onClick = { showLocationCurrencyPicker = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("store_change_location_button")
            ) {
              Text("${countryInfo.flagEmoji} ${countryInfo.currencyCode}")
            }
          }
        }

        // Toast or Error Banner
        val feedback = uiState.toastMessage ?: uiState.errorMessage
        if (feedback != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = if (uiState.errorMessage != null) {
                MaterialTheme.colorScheme.secondaryContainer
              } else {
                EucalyptusSage.copy(alpha = 0.18f)
              }
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = { viewModel.clearErrorMessage() }) {
                Text("OK")
              }
            }
          }
        }

        // Player Level & XP Progress Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrushedBrass.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = BrushedBrass
                  )
                }
                Column {
                  Text(
                    text = "Level $playerLevel Sovereign",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "$totalXp Total XP • ${unlockedIds.size} Items Unlocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Text(
                text = "$xpIntoLevel / 200 XP",
                style = MaterialTheme.typography.labelLarge,
                color = BrushedBrass,
                fontWeight = FontWeight.Bold
              )
            }

            LinearProgressIndicator(
              progress = { (xpIntoLevel.toFloat() / 200f).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = BrushedBrass,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
          }
        }

        // Section 1: In-App Purchases (Royal Coin Bundles)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = BrushedBrass
              )
              Column {
                Text(
                  text = "In-App Coin Packs (${countryInfo.currencyCode})",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Localized pricing for ${countryInfo.flagEmoji} ${countryInfo.name} • Play Billing Ready",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            RoyalStoreCatalog.IAP_BUNDLES.forEach { bundle ->
              val localizedPriceText = bundle.localizedPrice(activeCountryCode)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(
                      text = bundle.title,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    if (bundle.badgeTag.isNotBlank()) {
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(6.dp))
                          .background(BrushedBrass.copy(alpha = 0.22f))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Text(
                          text = bundle.badgeTag,
                          style = MaterialTheme.typography.labelSmall,
                          color = BrushedBrass,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    }
                  }
                  Text(
                    text = bundle.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                  onClick = { pendingIapItem = bundle },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BrushedBrass,
                    contentColor = Color.White
                  ),
                  modifier = Modifier.testTag("buy_iap_${bundle.id}")
                ) {
                  Text(
                    text = localizedPriceText,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        // Section 2: 3D Cosmetics & VIP Crest Unlocks (Spend Royal Coins)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = TerracottaRed
              )
              Column {
                Text(
                  text = "3D Atelier Cosmetics & VIP Unlocks",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Redeem Royal Coins earned from Daily Challenges or Matches",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            RoyalStoreCatalog.COSMETIC_ITEMS.forEach { item ->
              val isUnlocked = item.id in unlockedIds
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(
                      text = item.title,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = item.badgeTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrushedBrass
                      )
                    }
                  }
                  Text(
                    text = "${item.subtitle} (+${item.bonusXp} XP • ≈ ${CountryCatalog.formatCoinsEquivalent(item.coinCost, activeCountryCode)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                if (isUnlocked) {
                  OutlinedButton(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = EucalyptusSage,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Owned")
                  }
                } else {
                  Button(
                    onClick = { viewModel.purchaseOrUnlockStoreItem(item, profile) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("unlock_item_${item.id}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.MonetizationOn,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${item.coinCost}")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  val selectedBundle = pendingIapItem
  if (selectedBundle != null) {
    val localizedPriceText = selectedBundle.localizedPrice(activeCountryCode)
    AlertDialog(
      onDismissRequest = { pendingIapItem = null },
      title = { Text("Confirm In-App Purchase") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = selectedBundle.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "You will receive +${selectedBundle.coinsGranted} Royal Coins and +${selectedBundle.bonusXp} XP immediately.",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "Regional Price (${countryInfo.flagEmoji} ${countryInfo.currencyCode}): $localizedPriceText",
            style = MaterialTheme.typography.labelLarge,
            color = BrushedBrass
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.purchaseOrUnlockStoreItem(selectedBundle, profile)
            pendingIapItem = null
          },
          modifier = Modifier.testTag("confirm_iap_purchase_button")
        ) {
          Text("Buy Now ($localizedPriceText)")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingIapItem = null }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showLocationCurrencyPicker) {
    LocationCurrencyPickerDialog(
      selectedCountryCode = activeCountryCode,
      onSelectCountry = { code ->
        viewModel.updateUserCountry(code)
        showLocationCurrencyPicker = false
      },
      onDismiss = { showLocationCurrencyPicker = false }
    )
  }
}

@Composable
fun LocationCurrencyPickerDialog(
  selectedCountryCode: String,
  onSelectCountry: (String) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Select Player Location & Currency") },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(CountryCatalog.COUNTRIES, key = { it.code }) { country ->
          val isSelected = country.code == selectedCountryCode
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (isSelected) BrushedBrass.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant
              )
              .clickable { onSelectCountry(country.code) }
              .padding(horizontal = 14.dp, vertical = 10.dp)
              .testTag("currency_country_option_${country.code}"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = country.flagEmoji, style = MaterialTheme.typography.titleMedium)
              Column {
                Text(
                  text = country.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                  text = "Currency: ${country.currencyCode} (${country.currencySymbol.trim()})",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Text(
              text = "${country.currencySymbol.trim()} ${country.currencyCode}",
              style = MaterialTheme.typography.labelMedium,
              color = BrushedBrass,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun OwnerAdminControlScreen(
  uiState: LudoUiState,
  adminConfig: AdminAppConfig,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val context = LocalContext.current
  val defaultDbId = remember(context) { context.getString(R.string.firestore_database_id) }

  val isRecognizedOwnerEmail = remember(viewModel.currentUserEmail, adminConfig.ownerEmail) {
    val email = viewModel.currentUserEmail?.trim().orEmpty()
    email.equals("farhanSajid@gmail.com", ignoreCase = true) ||
      email.equals(adminConfig.ownerEmail, ignoreCase = true)
  }
  var adminUnlockedByPin by remember { mutableStateOf(isRecognizedOwnerEmail) }
  var pinInput by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf<String?>(null) }

  var ownerEmailInput by remember(adminConfig.ownerEmail) {
    mutableStateOf(adminConfig.ownerEmail.ifBlank { "farhanSajid@gmail.com" })
  }
  var customDbIdInput by remember(adminConfig.customFirebaseDatabaseId) {
    mutableStateOf(adminConfig.customFirebaseDatabaseId.ifBlank { defaultDbId })
  }
  var turnDurationInput by remember(adminConfig.turnDurationSeconds) {
    mutableLongStateOf(adminConfig.turnDurationSeconds)
  }
  var winPointsInput by remember(adminConfig.winPoints) {
    mutableLongStateOf(adminConfig.winPoints)
  }
  var capturePointsInput by remember(adminConfig.capturePoints) {
    mutableLongStateOf(adminConfig.capturePoints)
  }
  var homePointsInput by remember(adminConfig.homePoints) {
    mutableLongStateOf(adminConfig.homePoints)
  }
  var rewardMultiplierInput by remember(adminConfig.missionRewardMultiplier) {
    mutableLongStateOf(adminConfig.missionRewardMultiplier)
  }
  var announcementInput by remember(adminConfig.announcementBanner) {
    mutableStateOf(adminConfig.announcementBanner)
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
          .widthIn(max = 600.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = { viewModel.leaveToLobby() },
              modifier = Modifier.testTag("admin_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Lobby"
              )
            }
            Column {
              Text(
                text = "Owner Admin & Firebase Control",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Configure Firebase Database & Live Game Parameters",
                style = MaterialTheme.typography.labelMedium,
                color = BrushedBrass
              )
            }
          }

          Icon(
            imageVector = Icons.Default.AdminPanelSettings,
            contentDescription = null,
            tint = BrushedBrass,
            modifier = Modifier.size(28.dp)
          )
        }

        val feedback = uiState.toastMessage ?: uiState.errorMessage
        if (feedback != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = if (uiState.errorMessage != null) {
                MaterialTheme.colorScheme.secondaryContainer
              } else {
                EucalyptusSage.copy(alpha = 0.18f)
              }
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = { viewModel.clearErrorMessage() }) {
                Text("OK")
              }
            }
          }
        }

        if (!adminUnlockedByPin && !isRecognizedOwnerEmail) {
          // Owner PIN Gate Card (for non-owner accounts or offline studio mode)
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = TerracottaRed
                )
                Column {
                  Text(
                    text = "Owner Verification",
                    style = MaterialTheme.typography.titleLarge
                  )
                  Text(
                    text = "Sign in as ${adminConfig.ownerEmail} or enter Owner PIN (default: 2468)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              OutlinedTextField(
                value = pinInput,
                onValueChange = {
                  pinInput = it.take(8)
                  pinError = null
                },
                label = { Text("Owner Access PIN") },
                placeholder = { Text("Enter 2468") },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_pin_input")
              )

              if (pinError != null) {
                Text(
                  text = pinError!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = TerracottaRed
                )
              }

              Button(
                onClick = {
                  if (pinInput.trim() == "2468") {
                    adminUnlockedByPin = true
                  } else {
                    pinError = "Invalid Owner PIN (Hint: 2468)"
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("unlock_admin_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text("Unlock Owner Control Panel")
              }
            }
          }
        } else {
          // Section 1: Firebase Settings & Database Selector
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Storage,
                  contentDescription = null,
                  tint = BrushedBrass
                )
                Column {
                  Text(
                    text = "Firebase Backend Settings",
                    style = MaterialTheme.typography.titleLarge
                  )
                  Text(
                    text = if (viewModel.isAuthenticated) {
                      "Connected as ${viewModel.currentUserEmail ?: viewModel.currentUserId}"
                    } else {
                      "Local Preview Mode — Sign in with Google to sync to Firestore"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              OutlinedTextField(
                value = ownerEmailInput,
                onValueChange = { ownerEmailInput = it.take(100) },
                label = { Text("Designated Owner Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )

              OutlinedTextField(
                value = customDbIdInput,
                onValueChange = { customDbIdInput = it.take(120) },
                label = { Text("Firestore Database ID") },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_firebase_db_input")
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                FilterChip(
                  selected = customDbIdInput == defaultDbId,
                  onClick = { customDbIdInput = defaultDbId },
                  label = { Text("Provisioned DB") }
                )
                FilterChip(
                  selected = customDbIdInput == "(default)",
                  onClick = { customDbIdInput = "(default)" },
                  label = { Text("(default) DB") }
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = EucalyptusSage,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Collections: /users, /rooms, /rooms/{id}/voiceSignals, /appConfig/global",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          // Section 2: Live App & Game Rules Admin Control
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Text(
                text = "Live Game & Economy Parameters",
                style = MaterialTheme.typography.titleLarge
              )

              OutlinedTextField(
                value = announcementInput,
                onValueChange = { announcementInput = it.take(180) },
                label = { Text("Global Lobby Announcement Banner") },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("admin_announcement_input")
              )

              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

              // Turn Duration Selector
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Turn Timer Duration: ${turnDurationInput}s",
                  style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(15L, 22L, 30L, 45L).forEach { sec ->
                    FilterChip(
                      selected = turnDurationInput == sec,
                      onClick = { turnDurationInput = sec },
                      label = { Text("${sec}s") }
                    )
                  }
                }
              }

              // Daily Mission Reward Multiplier
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Daily Mission Reward Multiplier: ${rewardMultiplierInput}x",
                  style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  listOf(1L, 2L, 3L, 5L).forEach { mult ->
                    FilterChip(
                      selected = rewardMultiplierInput == mult,
                      onClick = { rewardMultiplierInput = mult },
                      label = { Text("${mult}x Rewards") }
                    )
                  }
                }
              }

              // Leaderboard Scoring Multipliers
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = winPointsInput.toString(),
                  onValueChange = { winPointsInput = it.toLongOrNull()?.coerceIn(10L, 1000L) ?: 100L },
                  label = { Text("Win Pts") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = capturePointsInput.toString(),
                  onValueChange = { capturePointsInput = it.toLongOrNull()?.coerceIn(5L, 500L) ?: 25L },
                  label = { Text("Capture Pts") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                  value = homePointsInput.toString(),
                  onValueChange = { homePointsInput = it.toLongOrNull()?.coerceIn(5L, 500L) ?: 15L },
                  label = { Text("Home Pts") },
                  singleLine = true,
                  modifier = Modifier.weight(1f)
                )
              }

              Button(
                onClick = {
                  viewModel.saveAdminConfiguration(
                    AdminAppConfig(
                      id = "global",
                      updatedByUid = viewModel.currentUserId.orEmpty(),
                      ownerEmail = ownerEmailInput,
                      customFirebaseDatabaseId = customDbIdInput,
                      turnDurationSeconds = turnDurationInput,
                      winPoints = winPointsInput,
                      capturePoints = capturePointsInput,
                      homePoints = homePointsInput,
                      missionRewardMultiplier = rewardMultiplierInput,
                      announcementBanner = announcementInput
                    )
                  )
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("save_admin_config_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = BrushedBrass,
                  contentColor = Color.White
                )
              ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Save Firebase & Admin Configuration",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}
