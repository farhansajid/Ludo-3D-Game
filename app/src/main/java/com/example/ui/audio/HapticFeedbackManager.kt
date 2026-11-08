package com.example.ui.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LudoHapticEvent {
  TURN_START,
  DICE_ROLL,
  PAWN_CAPTURE,
  DIE_SELECT,
  PAWN_MOVE,
  HOME_OR_VICTORY
}

data class HapticPatternSpec(
  val id: String,
  val event: LudoHapticEvent,
  val title: String,
  val description: String,
  val timingsMs: LongArray,
  val amplitudes: IntArray,
  val viewHapticConstant: Int
) {
  val totalDurationMs: Long
    get() = timingsMs.sum()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is HapticPatternSpec) return false
    return id == other.id &&
      event == other.event &&
      title == other.title &&
      description == other.description &&
      timingsMs.contentEquals(other.timingsMs) &&
      amplitudes.contentEquals(other.amplitudes) &&
      viewHapticConstant == other.viewHapticConstant
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + event.hashCode()
    result = 31 * result + timingsMs.contentHashCode()
    result = 31 * result + amplitudes.contentHashCode()
    return result
  }
}

interface HapticEffectRepository {
  val isHapticsEnabled: StateFlow<Boolean>
  val lastTriggeredPattern: StateFlow<HapticPatternSpec?>
  val triggeredEventsHistory: StateFlow<List<LudoHapticEvent>>

  fun getPattern(event: LudoHapticEvent): HapticPatternSpec
  fun getAllPatterns(): List<HapticPatternSpec>
  fun triggerHaptic(event: LudoHapticEvent): HapticPatternSpec?
  fun triggerTurnStart(): HapticPatternSpec? = triggerHaptic(LudoHapticEvent.TURN_START)
  fun triggerDiceRoll(): HapticPatternSpec? = triggerHaptic(LudoHapticEvent.DICE_ROLL)
  fun triggerPawnCapture(): HapticPatternSpec? = triggerHaptic(LudoHapticEvent.PAWN_CAPTURE)
  fun setHapticsEnabled(enabled: Boolean)
  fun toggleHaptics(): Boolean
  fun clearHistory()
}

class DefaultHapticEffectRepository : HapticEffectRepository {

  private val _isHapticsEnabled = MutableStateFlow(true)
  override val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

  private val _lastTriggeredPattern = MutableStateFlow<HapticPatternSpec?>(null)
  override val lastTriggeredPattern: StateFlow<HapticPatternSpec?> = _lastTriggeredPattern.asStateFlow()

  private val _triggeredEventsHistory = MutableStateFlow<List<LudoHapticEvent>>(emptyList())
  override val triggeredEventsHistory: StateFlow<List<LudoHapticEvent>> = _triggeredEventsHistory.asStateFlow()

  private val patternCatalog: Map<LudoHapticEvent, HapticPatternSpec> = mapOf(
    LudoHapticEvent.TURN_START to HapticPatternSpec(
      id = "haptic_turn_start",
      event = LudoHapticEvent.TURN_START,
      title = "Turn Awakening Double Pulse",
      description = "Crisp double tactile pulse alerting the player that their turn has begun",
      timingsMs = longArrayOf(0L, 18L, 45L, 28L),
      amplitudes = intArrayOf(0, 145, 0, 210),
      viewHapticConstant = HapticFeedbackConstants.VIRTUAL_KEY
    ),
    LudoHapticEvent.DICE_ROLL to HapticPatternSpec(
      id = "haptic_dice_roll",
      event = LudoHapticEvent.DICE_ROLL,
      title = "Dual-Dice Tabletop Rumble",
      description = "Multi-bounce tactile cadence matching the 3D dice tumble across the board",
      timingsMs = longArrayOf(0L, 16L, 28L, 18L, 30L, 22L, 34L, 30L),
      amplitudes = intArrayOf(0, 160, 0, 185, 0, 200, 0, 235),
      viewHapticConstant = HapticFeedbackConstants.KEYBOARD_TAP
    ),
    LudoHapticEvent.PAWN_CAPTURE to HapticPatternSpec(
      id = "haptic_pawn_capture",
      event = LudoHapticEvent.PAWN_CAPTURE,
      title = "Opponent Capture Impact Shockwave",
      description = "Heavy dual-impact shockwave when capturing an opponent's pawn",
      timingsMs = longArrayOf(0L, 42L, 36L, 68L, 30L, 38L),
      amplitudes = intArrayOf(0, 255, 0, 255, 0, 190),
      viewHapticConstant = HapticFeedbackConstants.LONG_PRESS
    ),
    LudoHapticEvent.DIE_SELECT to HapticPatternSpec(
      id = "haptic_die_select",
      event = LudoHapticEvent.DIE_SELECT,
      title = "Sequential Die Selection Tick",
      description = "Subtle precision tick when switching between Die #1 and Die #2",
      timingsMs = longArrayOf(0L, 12L),
      amplitudes = intArrayOf(0, 130),
      viewHapticConstant = HapticFeedbackConstants.CLOCK_TICK
    ),
    LudoHapticEvent.PAWN_MOVE to HapticPatternSpec(
      id = "haptic_pawn_move",
      event = LudoHapticEvent.PAWN_MOVE,
      title = "Pawn Tile Step Tap",
      description = "Light wooden tap when a 3D pawn advances along the track",
      timingsMs = longArrayOf(0L, 14L),
      amplitudes = intArrayOf(0, 145),
      viewHapticConstant = HapticFeedbackConstants.CLOCK_TICK
    ),
    LudoHapticEvent.HOME_OR_VICTORY to HapticPatternSpec(
      id = "haptic_home_or_victory",
      event = LudoHapticEvent.HOME_OR_VICTORY,
      title = "Royal Home & Victory Crescendo",
      description = "Ascending 4-pulse celebratory vibration when reaching Home Goal or winning",
      timingsMs = longArrayOf(0L, 24L, 35L, 32L, 35L, 55L, 40L, 85L),
      amplitudes = intArrayOf(0, 175, 0, 205, 0, 235, 0, 255),
      viewHapticConstant = HapticFeedbackConstants.LONG_PRESS
    )
  )

  override fun getPattern(event: LudoHapticEvent): HapticPatternSpec {
    return patternCatalog.getValue(event)
  }

  override fun getAllPatterns(): List<HapticPatternSpec> {
    return LudoHapticEvent.entries.map { getPattern(it) }
  }

  override fun triggerHaptic(event: LudoHapticEvent): HapticPatternSpec? {
    if (!_isHapticsEnabled.value) return null
    val spec = getPattern(event)
    _lastTriggeredPattern.value = spec
    _triggeredEventsHistory.update { history ->
      (history + event).takeLast(MAX_HISTORY_SIZE)
    }
    return spec
  }

  override fun setHapticsEnabled(enabled: Boolean) {
    _isHapticsEnabled.value = enabled
  }

  override fun toggleHaptics(): Boolean {
    val next = !_isHapticsEnabled.value
    _isHapticsEnabled.value = next
    if (next) {
      triggerHaptic(LudoHapticEvent.TURN_START)
    }
    return next
  }

  override fun clearHistory() {
    _lastTriggeredPattern.value = null
    _triggeredEventsHistory.value = emptyList()
  }

  companion object {
    const val MAX_HISTORY_SIZE = 64
  }
}

class HapticFeedbackManager(
  val repository: HapticEffectRepository = DefaultHapticEffectRepository()
) {
  val isHapticsEnabled: StateFlow<Boolean> = repository.isHapticsEnabled
  val lastTriggeredPattern: StateFlow<HapticPatternSpec?> = repository.lastTriggeredPattern
  val triggeredEventsHistory: StateFlow<List<LudoHapticEvent>> = repository.triggeredEventsHistory

  fun toggleHaptics(): Boolean = repository.toggleHaptics()

  fun setHapticsEnabled(enabled: Boolean) {
    repository.setHapticsEnabled(enabled)
  }

  fun trigger(event: LudoHapticEvent): HapticPatternSpec? {
    return repository.triggerHaptic(event)
  }

  fun triggerTurnBegin(): HapticPatternSpec? {
    return repository.triggerTurnStart()
  }

  fun triggerDiceRoll(): HapticPatternSpec? {
    return repository.triggerDiceRoll()
  }

  fun triggerPawnCapture(): HapticPatternSpec? {
    return repository.triggerPawnCapture()
  }

  fun clearHistory() {
    repository.clearHistory()
  }

  companion object {
    /**
     * Executes the hardware waveform vibration on the device's [Vibrator] / [VibratorManager]
     * and performs a synchronized [View.performHapticFeedback] pulse.
     */
    fun performHardwareHaptic(context: Context, view: View?, spec: HapticPatternSpec) {
      try {
        view?.performHapticFeedback(spec.viewHapticConstant)
      } catch (_: Throwable) {
        // Ignore view haptic errors in headless environments
      }

      try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          manager?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && vibrator.hasVibrator()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(spec.timingsMs, spec.amplitudes, -1)
            vibrator.vibrate(effect)
          } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(spec.timingsMs, -1)
          }
        }
      } catch (_: Throwable) {
        // Ignore hardware vibrator errors in headless/emulator environments
      }
    }
  }
}
