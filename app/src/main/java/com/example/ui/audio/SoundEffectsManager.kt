package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LudoSoundEvent {
  DICE_ROLL,
  MOVE,
  SAFE,
  CAPTURE,
  OPEN,
  HOME,
  GAME_CELEBRATION
}

data class SoundNoteSpec(
  val freqHz: Double,
  val durationMs: Int,
  val volume: Double,
  val secondHarmonic: Double = 0.28,
  val thirdHarmonic: Double = 0.08
)

data class SoundEffectClip(
  val id: String,
  val event: LudoSoundEvent,
  val title: String,
  val description: String,
  val notes: List<SoundNoteSpec>
) {
  val totalDurationMs: Int
    get() = notes.sumOf { it.durationMs }
}

/**
 * Simple Repository Pattern interface for retrieving, caching, and playing Ludo 3D sound clips
 * (including dice rolling, pawn movement, and game-ending celebrations).
 */
interface SoundEffectRepository {
  val isSoundEnabled: StateFlow<Boolean>
  val volume: StateFlow<Float>
  val lastPlayedClip: StateFlow<SoundEffectClip?>
  val playedClipsHistory: StateFlow<List<LudoSoundEvent>>

  fun getClip(event: LudoSoundEvent): SoundEffectClip
  fun getAllClips(): List<SoundEffectClip>
  fun getSynthesizedPcmBuffer(event: LudoSoundEvent): ShortArray
  fun playClip(event: LudoSoundEvent): SoundEffectClip?
  fun playDiceRoll(): SoundEffectClip? = playClip(LudoSoundEvent.DICE_ROLL)
  fun playPawnMovement(): SoundEffectClip? = playClip(LudoSoundEvent.MOVE)
  fun playGameEndingCelebration(): SoundEffectClip? = playClip(LudoSoundEvent.GAME_CELEBRATION)
  fun setSoundEnabled(enabled: Boolean)
  fun toggleSound(): Boolean
  fun setVolume(volume: Float)
  fun clearHistory()
}

/**
 * Default repository implementation that stores the acoustic clip catalog, synthesizes and caches
 * 16-bit PCM buffers per [LudoSoundEvent], and streams low-latency static [AudioTrack] playback.
 */
class DefaultSoundEffectRepository(
  private val enableHardwareAudio: Boolean = true
) : SoundEffectRepository {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _isSoundEnabled = MutableStateFlow(true)
  override val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

  private val _volume = MutableStateFlow(1.0f)
  override val volume: StateFlow<Float> = _volume.asStateFlow()

  private val _lastPlayedClip = MutableStateFlow<SoundEffectClip?>(null)
  override val lastPlayedClip: StateFlow<SoundEffectClip?> = _lastPlayedClip.asStateFlow()

  private val _playedClipsHistory = MutableStateFlow<List<LudoSoundEvent>>(emptyList())
  override val playedClipsHistory: StateFlow<List<LudoSoundEvent>> = _playedClipsHistory.asStateFlow()

  private val pcmCache = ConcurrentHashMap<LudoSoundEvent, ShortArray>()

  private val clipCatalog: Map<LudoSoundEvent, SoundEffectClip> = mapOf(
    LudoSoundEvent.DICE_ROLL to SoundEffectClip(
      id = "clip_dice_roll",
      event = LudoSoundEvent.DICE_ROLL,
      title = "Dual-Dice Tabletop Roll",
      description = "Multi-bounce ivory dice clatter and wooden tabletop settle",
      notes = listOf(
        SoundNoteSpec(freqHz = 410.0, durationMs = 26, volume = 0.34, secondHarmonic = 0.35),
        SoundNoteSpec(freqHz = 545.0, durationMs = 24, volume = 0.36, secondHarmonic = 0.38),
        SoundNoteSpec(freqHz = 465.0, durationMs = 26, volume = 0.38, secondHarmonic = 0.34),
        SoundNoteSpec(freqHz = 610.0, durationMs = 28, volume = 0.42, secondHarmonic = 0.30),
        SoundNoteSpec(freqHz = 490.0, durationMs = 36, volume = 0.46, secondHarmonic = 0.25)
      )
    ),
    LudoSoundEvent.MOVE to SoundEffectClip(
      id = "clip_pawn_move",
      event = LudoSoundEvent.MOVE,
      title = "Pawn Step Hop",
      description = "Crisp tactile wooden pawn hop across board tiles",
      notes = listOf(
        SoundNoteSpec(freqHz = 590.0, durationMs = 26, volume = 0.38, secondHarmonic = 0.30),
        SoundNoteSpec(freqHz = 740.0, durationMs = 32, volume = 0.42, secondHarmonic = 0.24)
      )
    ),
    LudoSoundEvent.OPEN to SoundEffectClip(
      id = "clip_yard_open",
      event = LudoSoundEvent.OPEN,
      title = "Yard Deployment Fanfare",
      description = "Ascending royal brass triad when deploying a pawn on a 6",
      notes = listOf(
        SoundNoteSpec(freqHz = 523.25, durationMs = 68, volume = 0.46), // C5
        SoundNoteSpec(freqHz = 659.25, durationMs = 74, volume = 0.52), // E5
        SoundNoteSpec(freqHz = 783.99, durationMs = 128, volume = 0.58) // G5
      )
    ),
    LudoSoundEvent.SAFE to SoundEffectClip(
      id = "clip_safe_star",
      event = LudoSoundEvent.SAFE,
      title = "Safe Star & Shield Chime",
      description = "Warm crystalline double chime when landing on a Safe Star or Blockade",
      notes = listOf(
        SoundNoteSpec(freqHz = 659.25, durationMs = 78, volume = 0.46), // E5
        SoundNoteSpec(freqHz = 987.77, durationMs = 148, volume = 0.52) // B5
      )
    ),
    LudoSoundEvent.CAPTURE to SoundEffectClip(
      id = "clip_pawn_capture",
      event = LudoSoundEvent.CAPTURE,
      title = "Tactical Capture Strike",
      description = "Dramatic impact strike when capturing an opponent's pawn",
      notes = listOf(
        SoundNoteSpec(freqHz = 311.13, durationMs = 65, volume = 0.62, secondHarmonic = 0.40), // D#4
        SoundNoteSpec(freqHz = 220.00, durationMs = 80, volume = 0.66, secondHarmonic = 0.42), // A3
        SoundNoteSpec(freqHz = 440.00, durationMs = 140, volume = 0.58, secondHarmonic = 0.35) // A4
      )
    ),
    LudoSoundEvent.HOME to SoundEffectClip(
      id = "clip_home_goal",
      event = LudoSoundEvent.HOME,
      title = "Home Goal Arpeggio",
      description = "Ascending 4-note harmonic chime when a pawn reaches the Home triangle",
      notes = listOf(
        SoundNoteSpec(freqHz = 523.25, durationMs = 80, volume = 0.50), // C5
        SoundNoteSpec(freqHz = 659.25, durationMs = 80, volume = 0.52), // E5
        SoundNoteSpec(freqHz = 783.99, durationMs = 90, volume = 0.56), // G5
        SoundNoteSpec(freqHz = 1046.50, durationMs = 200, volume = 0.62) // C6
      )
    ),
    LudoSoundEvent.GAME_CELEBRATION to SoundEffectClip(
      id = "clip_game_celebration",
      event = LudoSoundEvent.GAME_CELEBRATION,
      title = "Royal Victory Celebration Fanfare",
      description = "Grand 6-note celebratory brass fanfare for match and tournament victories",
      notes = listOf(
        SoundNoteSpec(freqHz = 523.25, durationMs = 90, volume = 0.56, secondHarmonic = 0.36, thirdHarmonic = 0.14), // C5
        SoundNoteSpec(freqHz = 659.25, durationMs = 90, volume = 0.58, secondHarmonic = 0.36, thirdHarmonic = 0.14), // E5
        SoundNoteSpec(freqHz = 783.99, durationMs = 95, volume = 0.62, secondHarmonic = 0.38, thirdHarmonic = 0.15), // G5
        SoundNoteSpec(freqHz = 1046.50, durationMs = 155, volume = 0.68, secondHarmonic = 0.40, thirdHarmonic = 0.16), // C6
        SoundNoteSpec(freqHz = 783.99, durationMs = 85, volume = 0.60, secondHarmonic = 0.36, thirdHarmonic = 0.14), // G5
        SoundNoteSpec(freqHz = 1046.50, durationMs = 280, volume = 0.72, secondHarmonic = 0.42, thirdHarmonic = 0.18)  // C6 sustain
      )
    )
  )

  override fun getClip(event: LudoSoundEvent): SoundEffectClip {
    return clipCatalog.getValue(event)
  }

  override fun getAllClips(): List<SoundEffectClip> {
    return LudoSoundEvent.entries.map { getClip(it) }
  }

  override fun getSynthesizedPcmBuffer(event: LudoSoundEvent): ShortArray {
    return pcmCache.getOrPut(event) {
      synthesizeClipPcm(getClip(event))
    }
  }

  override fun playClip(event: LudoSoundEvent): SoundEffectClip? {
    if (!_isSoundEnabled.value) return null
    val clip = getClip(event)
    val gain = _volume.value.coerceIn(0f, 1f)
    if (gain <= 0f) return null

    _lastPlayedClip.value = clip
    _playedClipsHistory.update { history ->
      (history + event).takeLast(MAX_HISTORY_SIZE)
    }

    // Ensure PCM buffer is synthesized & cached in repository
    val basePcm = getSynthesizedPcmBuffer(event)

    if (enableHardwareAudio) {
      scope.launch {
        try {
          playPcmBuffer(basePcm, clip.totalDurationMs, gain)
        } catch (_: Throwable) {
          // Ignore audio hardware availability errors on headless/test environments
        }
      }
    }
    return clip
  }

  override fun setSoundEnabled(enabled: Boolean) {
    _isSoundEnabled.value = enabled
  }

  override fun toggleSound(): Boolean {
    val next = !_isSoundEnabled.value
    _isSoundEnabled.value = next
    if (next) {
      playClip(LudoSoundEvent.SAFE)
    }
    return next
  }

  override fun setVolume(volume: Float) {
    _volume.value = volume.coerceIn(0f, 1f)
  }

  override fun clearHistory() {
    _lastPlayedClip.value = null
    _playedClipsHistory.value = emptyList()
  }

  private fun synthesizeClipPcm(clip: SoundEffectClip): ShortArray {
    val totalSamples = clip.notes.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
    if (totalSamples <= 0) return ShortArray(0)

    val pcmBuffer = ShortArray(totalSamples)
    var writeCursor = 0

    for (note in clip.notes) {
      val count = (SAMPLE_RATE * note.durationMs) / 1000
      for (i in 0 until count) {
        val t = i.toDouble() / SAMPLE_RATE
        // Quick attack + exponential decay envelope for warm acoustic timbre
        val attack = (i.toDouble() / (SAMPLE_RATE * 0.004)).coerceAtMost(1.0)
        val decay = exp(-5.8 * i.toDouble() / count.toDouble())
        val fundamental = sin(2.0 * PI * note.freqHz * t)
        val second = note.secondHarmonic * sin(2.0 * PI * (note.freqHz * 2.0) * t)
        val third = note.thirdHarmonic * sin(2.0 * PI * (note.freqHz * 3.0) * t)
        val norm = 1.0 + note.secondHarmonic + note.thirdHarmonic
        val sample = ((fundamental + second + third) / norm) * attack * decay * note.volume
        val clamped = (sample * Short.MAX_VALUE).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        if (writeCursor < pcmBuffer.size) {
          pcmBuffer[writeCursor++] = clamped.toShort()
        }
      }
    }
    return pcmBuffer
  }

  private fun playPcmBuffer(basePcm: ShortArray, totalDurationMs: Int, gain: Float) {
    if (basePcm.isEmpty()) return
    val scaledPcm = if (gain >= 0.99f) {
      basePcm
    } else {
      ShortArray(basePcm.size) { idx ->
        (basePcm[idx] * gain).toInt()
          .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
          .toShort()
      }
    }

    val byteCount = scaledPcm.size * 2
    val minBuf = AudioTrack.getMinBufferSize(
      SAMPLE_RATE,
      AudioFormat.CHANNEL_OUT_MONO,
      AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(byteCount)

    val track = AudioTrack.Builder()
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      .setAudioFormat(
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(SAMPLE_RATE)
          .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
          .build()
      )
      .setTransferMode(AudioTrack.MODE_STATIC)
      .setBufferSizeInBytes(minBuf)
      .build()

    track.write(scaledPcm, 0, scaledPcm.size)
    track.play()
    Thread.sleep(totalDurationMs.toLong() + 45L)
    track.stop()
    track.release()
  }

  companion object {
    const val SAMPLE_RATE = 24000
    const val MAX_HISTORY_SIZE = 64
  }
}

/**
 * Sound effect manager using a simple [SoundEffectRepository] pattern to play synthesized
 * acoustic clips for dice rolling, pawn movement, tactical board events, and game-ending celebrations.
 */
class SoundEffectsManager(
  val repository: SoundEffectRepository = DefaultSoundEffectRepository()
) {
  val isSoundEnabled: StateFlow<Boolean> = repository.isSoundEnabled
  val volume: StateFlow<Float> = repository.volume
  val lastPlayedClip: StateFlow<SoundEffectClip?> = repository.lastPlayedClip
  val playedClipsHistory: StateFlow<List<LudoSoundEvent>> = repository.playedClipsHistory

  fun toggleSound(): Boolean = repository.toggleSound()

  fun setSoundEnabled(enabled: Boolean) {
    repository.setSoundEnabled(enabled)
  }

  fun setVolume(volume: Float) {
    repository.setVolume(volume)
  }

  fun play(event: LudoSoundEvent): SoundEffectClip? {
    return repository.playClip(event)
  }

  fun playDiceRoll(): SoundEffectClip? {
    return repository.playDiceRoll()
  }

  fun playPawnMove(): SoundEffectClip? {
    return repository.playPawnMovement()
  }

  fun playGameCelebration(): SoundEffectClip? {
    return repository.playGameEndingCelebration()
  }

  fun clearHistory() {
    repository.clearHistory()
  }
}
