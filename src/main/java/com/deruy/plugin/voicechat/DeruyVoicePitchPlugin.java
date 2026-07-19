package com.deruy.plugin.voicechat;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.PitchShifter;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 투명화 상태인 플레이어의 마이크 오디오를 가로채서
 * 고정 피치 다운시프트(기본 -6반음, 성인 남성 저음 느낌)를 적용하는
 * SimpleVoiceChat 서버 플러그인.
 *
 * 투명화가 아닌 평소 상태에서는 아무 처리도 하지 않고 그대로 통과시켜
 * 서버 부하를 최소화함.
 */
public class DeruyVoicePitchPlugin implements VoicechatPlugin {

    // 몇 반음 낮출지. 음수 = 다운피치. -4~-8 사이에서 취향껏 조절 권장.
    private static final double PITCH_SEMITONES = -6.0;
    private static final double PITCH_FACTOR = Math.pow(2.0, PITCH_SEMITONES / 12.0);

    private static final int SAMPLE_RATE = 48000;
    private static final int BUFFER_SIZE = 960; // 20ms @ 48kHz mono
    private static final int OVERLAP = 240;

    private static final Logger LOGGER = Logger.getLogger("DeruyVoicePitchPlugin");

    private final InvisibilityVoiceEffectTracker tracker;

    // 플레이어별 상태. opus 코덱과 피치 이펙트는 프레임 간 연속성이 있어야 하므로
    // 매 패킷마다 새로 만들지 않고 재사용해야 함.
    private final ConcurrentHashMap<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, OpusEncoder> encoders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PitchShifter> pitchEffects = new ConcurrentHashMap<>();

    private VoicechatApi api;

    public DeruyVoicePitchPlugin(InvisibilityVoiceEffectTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public String getPluginId() {
        return "deruyplugin-voicepitch";
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null || event.getSenderConnection().getPlayer() == null) {
            return;
        }

        UUID playerId = event.getSenderConnection().getPlayer().getUuid();

        if (!tracker.isActive(playerId)) {
            return; // 투명화 아니면 원본 그대로 통과
        }

        try {
            byte[] opusData = event.getPacket().getOpusEncodedData();
            if (opusData == null || opusData.length == 0) {
                return;
            }

            OpusDecoder decoder = decoders.computeIfAbsent(playerId, id -> api.createDecoder());
            OpusEncoder encoder = encoders.computeIfAbsent(playerId, id -> api.createEncoder());
            PitchShifter pitchEffect = pitchEffects.computeIfAbsent(playerId,
                    id -> new PitchShifter(PITCH_FACTOR, SAMPLE_RATE, BUFFER_SIZE, OVERLAP));

            short[] pcm = decoder.decode(opusData);

            TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(SAMPLE_RATE, 16, 1, true, false);
            AudioEvent audioEvent = new AudioEvent(format);
            audioEvent.setFloatBuffer(shortsToFloats(pcm));

            pitchEffect.process(audioEvent);

            short[] processedPcm = floatsToShorts(audioEvent.getFloatBuffer());
            byte[] newOpus = encoder.encode(processedPcm);
            event.getPacket().setOpusEncodedData(newOpus);
        } catch (Exception e) {
            // 처리 실패해도 목소리가 아예 끊기는 것보단 원본 오디오가 그대로 나가는 게 나음.
            LOGGER.log(Level.WARNING, "투명화 피치 이펙트 처리 중 오류 (플레이어: " + playerId + ")", e);
        }
    }

    /**
     * 플레이어가 투명화를 해제했을 때 호출. 코덱/이펙트 상태를 정리해서
     * 메모리 누수를 막고, 다음에 다시 투명화했을 때 새 상태로 시작하게 함.
     */
    public void cleanupPlayer(UUID playerId) {
        decoders.remove(playerId);
        encoders.remove(playerId);
        pitchEffects.remove(playerId);
    }

    private static float[] shortsToFloats(short[] shorts) {
        float[] floats = new float[shorts.length];
        for (int i = 0; i < shorts.length; i++) {
            floats[i] = shorts[i] / 32768f;
        }
        return floats;
    }

    private static short[] floatsToShorts(float[] floats) {
        short[] shorts = new short[floats.length];
        for (int i = 0; i < floats.length; i++) {
            float v = Math.max(-1f, Math.min(1f, floats[i]));
            shorts[i] = (short) (v * 32767f);
        }
        return shorts;
    }
}
