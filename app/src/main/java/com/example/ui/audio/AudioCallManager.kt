package com.example.ui.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.example.data.AudioCallMode
import com.example.data.LudoMultiplayerRepository
import com.example.data.VoiceSignal
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioCallUiState(
  val isInCall: Boolean = false,
  val callMode: AudioCallMode = AudioCallMode.NONE,
  val targetPeerUid: String = "ALL",
  val targetPeerName: String = "Room Table",
  val isMicMuted: Boolean = false,
  val isSpeakerEnabled: Boolean = true,
  val mutedPeerUids: Set<String> = emptySet(),
  val speakingUids: Set<String> = emptySet(),
  val localAudioLevel: Float = 0f,
  val hasMicPermission: Boolean = false
)

/**
 * Real-time low-latency voice engine supporting both Group Room Voice Calls ("ALL")
 * and 1-to-1 Direct Peer Audio Calls over Firestore voiceSignals transport with
 * AudioRecord capture, RMS voice activity detection, and AudioTrack PCM playback.
 */
class AudioCallManager(
  private val repository: LudoMultiplayerRepository,
  private val scope: CoroutineScope,
  private val currentUserId: String?
) {

  private val _callState = MutableStateFlow(AudioCallUiState())
  val callState: StateFlow<AudioCallUiState> = _callState.asStateFlow()

  private var captureJob: Job? = null
  private var signalListenerJob: Job? = null
  private var audioTrack: AudioTrack? = null
  private val processedSignalIds = LinkedHashSet<String>()

  fun onMicPermissionResult(granted: Boolean) {
    _callState.update { it.copy(hasMicPermission = granted) }
  }

  fun startGroupCall(roomCode: String, displayName: String, currentActiveVoiceUids: List<String>) {
    joinCallInternal(
      roomCode = roomCode,
      displayName = displayName,
      mode = AudioCallMode.GROUP,
      targetUid = "ALL",
      targetName = "Group Table Call",
      currentActiveVoiceUids = currentActiveVoiceUids
    )
  }

  fun startOneToOneCall(
    roomCode: String,
    displayName: String,
    peerUid: String,
    peerName: String,
    currentActiveVoiceUids: List<String>
  ) {
    joinCallInternal(
      roomCode = roomCode,
      displayName = displayName,
      mode = AudioCallMode.DIRECT,
      targetUid = peerUid,
      targetName = peerName,
      currentActiveVoiceUids = currentActiveVoiceUids
    )
  }

  private fun joinCallInternal(
    roomCode: String,
    displayName: String,
    mode: AudioCallMode,
    targetUid: String,
    targetName: String,
    currentActiveVoiceUids: List<String>
  ) {
    _callState.update {
      it.copy(
        isInCall = true,
        callMode = mode,
        targetPeerUid = targetUid,
        targetPeerName = targetName,
        isMicMuted = false
      )
    }

    if (!currentUserId.isNullOrBlank() && roomCode.isNotBlank() && roomCode != "LOCAL-3D") {
      val updatedVoiceUids = (currentActiveVoiceUids + currentUserId).distinct()
      scope.launch {
        repository.updateVoiceCallState(
          roomCode = roomCode,
          activeVoiceUids = updatedVoiceUids,
          callMode = mode,
          callInitiatorUid = currentUserId,
          callTargetUid = targetUid
        )
        repository.sendVoiceSignal(
          roomCode = roomCode,
          senderName = displayName,
          targetUid = targetUid,
          signalType = "OFFER",
          payload = "VOICE_JOIN:${mode.name}"
        )
      }
      startListeningToRoomSignals(roomCode)
    }

    startAudioCaptureLoop(roomCode, displayName)
  }

  fun endCall(roomCode: String, displayName: String, currentActiveVoiceUids: List<String>) {
    captureJob?.cancel()
    captureJob = null
    signalListenerJob?.cancel()
    signalListenerJob = null
    releaseAudioTrack()

    val previousTarget = _callState.value.targetPeerUid
    _callState.update {
      it.copy(
        isInCall = false,
        callMode = AudioCallMode.NONE,
        targetPeerUid = "ALL",
        targetPeerName = "Room Table",
        speakingUids = emptySet(),
        localAudioLevel = 0f
      )
    }

    if (!currentUserId.isNullOrBlank() && roomCode.isNotBlank() && roomCode != "LOCAL-3D") {
      val remainingUids = currentActiveVoiceUids.filter { it != currentUserId }
      val nextMode = if (remainingUids.isEmpty()) AudioCallMode.NONE else AudioCallMode.GROUP
      scope.launch {
        repository.updateVoiceCallState(
          roomCode = roomCode,
          activeVoiceUids = remainingUids,
          callMode = nextMode,
          callInitiatorUid = "",
          callTargetUid = ""
        )
        repository.sendVoiceSignal(
          roomCode = roomCode,
          senderName = displayName,
          targetUid = previousTarget,
          signalType = "HANGUP",
          payload = "VOICE_LEAVE"
        )
      }
    }
  }

  fun toggleMicMute() {
    _callState.update { state ->
      val nextMuted = !state.isMicMuted
      val speaking = if (nextMuted && currentUserId != null) {
        state.speakingUids - currentUserId
      } else {
        state.speakingUids
      }
      state.copy(isMicMuted = nextMuted, speakingUids = speaking, localAudioLevel = 0f)
    }
  }

  fun toggleSpeaker() {
    _callState.update { it.copy(isSpeakerEnabled = !it.isSpeakerEnabled) }
  }

  fun toggleMutePeer(peerUid: String) {
    _callState.update { state ->
      val nextSet = if (peerUid in state.mutedPeerUids) {
        state.mutedPeerUids - peerUid
      } else {
        state.mutedPeerUids + peerUid
      }
      state.copy(mutedPeerUids = nextSet)
    }
  }

  private fun startListeningToRoomSignals(roomCode: String) {
    signalListenerJob?.cancel()
    signalListenerJob = scope.launch(Dispatchers.IO) {
      repository.observeVoiceSignals(roomCode)
        .catch { e -> Log.w(TAG, "Voice signal stream error", e) }
        .collect { signals ->
          for (sig in signals) {
            if (sig.id.isNotBlank() && !processedSignalIds.add(sig.id)) continue
            if (processedSignalIds.size > 200) {
              val first = processedSignalIds.firstOrNull()
              if (first != null) processedSignalIds.remove(first)
            }
            handleIncomingSignal(sig)
          }
        }
    }
  }

  private fun handleIncomingSignal(signal: VoiceSignal) {
    val myUid = currentUserId ?: return
    if (signal.senderUid == myUid) return

    val state = _callState.value
    if (!state.isInCall) return
    if (signal.senderUid in state.mutedPeerUids) return

    // Filter 1-to-1 private call frames vs group call frames
    val isForMe = signal.targetUid == "ALL" || signal.targetUid == myUid
    if (!isForMe) return
    if (state.callMode == AudioCallMode.DIRECT &&
      state.targetPeerUid != "ALL" &&
      signal.senderUid != state.targetPeerUid
    ) {
      return
    }

    when (signal.signalType) {
      "AUDIO_FRAME" -> {
        _callState.update { it.copy(speakingUids = it.speakingUids + signal.senderUid) }
        if (state.isSpeakerEnabled && signal.payload.isNotBlank()) {
          playEncodedPcmFrame(signal.payload)
        }
        scope.launch {
          delay(650L)
          _callState.update { it.copy(speakingUids = it.speakingUids - signal.senderUid) }
        }
      }
      "HANGUP" -> {
        _callState.update { it.copy(speakingUids = it.speakingUids - signal.senderUid) }
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun startAudioCaptureLoop(roomCode: String, displayName: String) {
    captureJob?.cancel()
    captureJob = scope.launch(Dispatchers.IO) {
      val sampleRate = 8000
      val minBuf = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )

      var recorder: AudioRecord? = null
      if (_callState.value.hasMicPermission && minBuf > 0) {
        try {
          val candidate = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf.coerceAtLeast(1600)
          )
          if (candidate.state == AudioRecord.STATE_INITIALIZED) {
            candidate.startRecording()
            recorder = candidate
          } else {
            candidate.release()
          }
        } catch (e: Exception) {
          Log.w(TAG, "AudioRecord unavailable on this device, using voice activity monitor", e)
        }
      }

      val shortBuffer = ShortArray(800) // 100ms frame at 8kHz
      var lastSentPacketMs = 0L

      try {
        while (isActive && _callState.value.isInCall) {
          val state = _callState.value
          if (state.isMicMuted) {
            delay(150L)
            continue
          }

          if (recorder != null) {
            val readCount = recorder.read(shortBuffer, 0, shortBuffer.size)
            if (readCount > 0) {
              var sumSq = 0.0
              for (i in 0 until readCount) {
                val sample = shortBuffer[i].toDouble() / 32768.0
                sumSq += sample * sample
              }
              val rms = sqrt(sumSq / readCount).toFloat().coerceIn(0f, 1f)
              val normalizedLevel = (rms * 4.5f).coerceIn(0f, 1f)
              val myUid = currentUserId ?: "LOCAL_GUEST"

              _callState.update {
                val nextSpeaking = if (normalizedLevel > 0.08f) {
                  it.speakingUids + myUid
                } else {
                  it.speakingUids - myUid
                }
                it.copy(localAudioLevel = normalizedLevel, speakingUids = nextSpeaking)
              }

              val now = System.currentTimeMillis()
              if (normalizedLevel > 0.08f &&
                !currentUserId.isNullOrBlank() &&
                roomCode.isNotBlank() &&
                roomCode != "LOCAL-3D" &&
                now - lastSentPacketMs >= 450L
              ) {
                lastSentPacketMs = now
                val byteBuf = ByteArray(readCount * 2)
                for (i in 0 until readCount) {
                  val v = shortBuffer[i].toInt()
                  byteBuf[i * 2] = (v and 0xFF).toByte()
                  byteBuf[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                }
                val encoded = Base64.encodeToString(byteBuf, Base64.NO_WRAP)
                repository.sendVoiceSignal(
                  roomCode = roomCode,
                  senderName = displayName,
                  targetUid = state.targetPeerUid,
                  signalType = "AUDIO_FRAME",
                  payload = encoded
                )
              }
            } else {
              delay(80L)
            }
          } else {
            delay(200L)
          }
        }
      } finally {
        try {
          recorder?.stop()
        } catch (_: Exception) {
        }
        try {
          recorder?.release()
        } catch (_: Exception) {
        }
      }
    }
  }

  private fun playEncodedPcmFrame(base64Payload: String) {
    try {
      val pcmBytes = Base64.decode(base64Payload, Base64.NO_WRAP)
      if (pcmBytes.isEmpty()) return
      val track = getOrCreateAudioTrack(pcmBytes.size) ?: return
      track.write(pcmBytes, 0, pcmBytes.size)
    } catch (e: Exception) {
      Log.w(TAG, "Unable to play incoming PCM voice frame", e)
    }
  }

  private fun getOrCreateAudioTrack(minSize: Int): AudioTrack? {
    val existing = audioTrack
    if (existing != null && existing.state == AudioTrack.STATE_INITIALIZED) {
      return existing
    }
    return try {
      val sampleRate = 8000
      val bufSize = maxOf(
        minSize,
        AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        )
      ).coerceAtLeast(1600)

      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufSize)
        .build()

      if (track.state == AudioTrack.STATE_INITIALIZED) {
        track.play()
        audioTrack = track
        track
      } else {
        track.release()
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "AudioTrack init failed", e)
      null
    }
  }

  private fun releaseAudioTrack() {
    try {
      audioTrack?.stop()
    } catch (_: Exception) {
    }
    try {
      audioTrack?.release()
    } catch (_: Exception) {
    }
    audioTrack = null
  }

  fun cleanup() {
    captureJob?.cancel()
    signalListenerJob?.cancel()
    releaseAudioTrack()
  }

  private companion object {
    const val TAG = "AudioCallManager"
  }
}
