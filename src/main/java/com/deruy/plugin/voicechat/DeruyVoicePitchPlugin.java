package com.deruy.plugin.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 투명화 상태인 플레이어의 마이크 오디오를 가로채서
 * 고정 피치 다운시프트(기본 -6반음, 성인 남성 저음 느낌 = "오토튠")를 적용하는
 * SimpleVoiceChat 서버 플러그인.
 *
 * 직접 구현한 리샘플링 방식의 피치시프트 + 플레이어별 FIFO 버퍼를 사용한다.
 * (기존에 쓰던 TarsosDSP의 PitchShifter는 패킷 단위로 끊어서 넣으면
 *  내부 상태가 꼬여서 무음에 가까운 결과가 나오는 문제가 있어 제거함)
 *
 * VoiceFeatureSettings.isAutotuneEnabled()로 서버 전체 온/오프 제어.
 * 꺼져 있거나 투명화가 아니면 원본 오디오를 그대로 통과시켜 서버 부하를 최소화한다.
 */
public class DeruyVoicePitchPlugin implements VoicechatPlugin {

    // 몇 반음 낮출지. 음수 = 다운피치. -4~-8 사이에서 취향껏 조절 권장.
    private static final double PITCH_SEMITONES = -6.0;
    private static final double PITCH_FACTOR = Math.pow(2.0, PITCH_SEMITONES / 12.0);

    private static final int BUFFER_SIZE = 960; // 20ms @ 48kHz mono, Opus 프레임 크기
    // FIFO가 무한정 커지는 걸 막기 위한 상한 (피치를 낮추면 원리상 계속 버퍼가 쌓이므로 필요)
    private static final int MAX_FIFO_SIZE = BUFFER_SIZE * 8;

    private static final Logger LOGGER = Logger.getLogger("DeruyVoicePitchPlugin");

    private final InvisibilityVoiceEffectTracker tracker;
    private final VoiceFeatureSettings settings;

    private final ConcurrentHashMap<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, OpusEncoder> encoders = new ConcurrentHashMap<>();
    // 플레이어별 리샘플된 오디오 FIFO. 매 패킷마다 리샘플 결과를 여기 쌓고, 정확히 960개씩 꺼내 인코딩한다.
    private final ConcurrentHashMap<UUID, Deque<Float>> fifoBuffers = new ConcurrentHashMap<>();

    private VoicechatApi api;

    public DeruyVoicePitchPlugin(InvisibilityVoiceEffectTracker tracker, VoiceFeatureSettings settings) {
        this.tracker = tracker;
        this.settings = settings;
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
        if (!settings.isAutotuneEnabled()) {
            return;
        }

        if (event.getSenderConnection() == null || event.getSenderConnection().getPlayer() == null) {
            return;
        }

        UUID playerId = event.getSenderConnection().getPlayer().getUuid();

        if (!tracker.isActive(playerId)) {
            return;
        }

        try {
            byte[] opusData = event.getPacket().getOpusEncodedData();
            if (opusData == null || opusData.length == 0) {
                return;
            }

            OpusDecoder decoder = decoders.computeIfAbsent(playerId, id -> api.createDecoder());
            OpusEncoder encoder = encoders.computeIfAbsent(playerId, id -> api.createEncoder());
            Deque<Float> fifo = fifoBuffers.computeIfAbsent(playerId, id -> new ArrayDeque<>());

            short[] pcm = decoder.decode(opusData);
            if (pcm == null || pcm.length == 0) return;

            // 리샘플링으로 피치 다운시프트: 원본보다 더 많은 샘플로 "늘려서" 해석하면
            // 재생시 더 낮은 음으로 들린다.
            float[] resampled = resamplePitchDown(pcm, PITCH_FACTOR);
            for (float sample : resampled) {
                fifo.addLast(sample);
            }

            // FIFO가 너무 커지지 않게 상한 유지 (오래된 것부터 버림, 약간의 지연은 감수)
            while (fifo.size() > MAX_FIFO_SIZE) {
                fifo.pollFirst();
            }

            // FIFO에 정확히 한 프레임(960개) 이상 쌓였을 때만 인코딩해서 내보낸다.
            if (fifo.size() < BUFFER_SIZE) {
                // 아직 한 프레임 분량이 안 모였으면 이번 패킷은 무음(0)으로 채워 내보내서
                // 타이밍은 유지하고, 다음 패킷들에서 쌓인 걸 내보낸다.
                return;
            }

            short[] outputPcm = new short[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                float v = fifo.pollFirst();
                v = Math.max(-1f, Math.min(1f, v));
                outputPcm[i] = (short) (v * 32767f);
            }

            byte[] newOpus = encoder.encode(outputPcm);
            if (newOpus == null || newOpus.length == 0) {
                return;
            }

            event.getPacket().setOpusEncodedData(newOpus);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "투명화 오토튠 이펙트 처리 중 오류 (플레이어: " + playerId + ")", e);
        }
    }

    /**
     * 선형보간 리샘플링으로 피치를 낮춘다. factor(0~1)가 작을수록 더 많이 낮아진다.
     * 출력 길이 = 입력길이 / factor (factor<1 이므로 출력이 더 길어짐 = FIFO에 더 많이 쌓임)
     */
    private static float[] resamplePitchDown(short[] input, double factor) {
        int outputLength = (int) Math.round(input.length / factor);
        float[] output = new float[outputLength];
        for (int i = 0; i < outputLength; i++) {
            double srcPos = i * factor;
            int idx = (int) srcPos;
            double frac = srcPos - idx;
            float a = idx < input.length ? input[idx] / 32768f : 0f;
            float b = (idx + 1) < input.length ? input[idx + 1] / 32768f : a;
            output[i] = (float) (a + (b - a) * frac);
        }
        return output;
    }

    /**
     * 플레이어가 투명화를 해제했을 때 호출. 코덱/버퍼 상태를 정리해서
     * 메모리 누수를 막고, 다음에 다시 투명화했을 때 새 상태로 시작하게 함.
     */
    public void cleanupPlayer(UUID playerId) {
        decoders.remove(playerId);
        encoders.remove(playerId);
        fifoBuffers.remove(playerId);
    }
}
