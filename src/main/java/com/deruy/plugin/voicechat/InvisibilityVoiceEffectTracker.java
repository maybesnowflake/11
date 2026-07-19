package com.deruy.plugin.voicechat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 플레이어의 투명화(INVISIBILITY) 상태를 추적하는 리스너.
 * DeruyVoicePitchPlugin이 이 값을 참조해서 음성 피치 이펙트를 켜고 끔.
 */
public class InvisibilityVoiceEffectTracker implements Listener {

    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private DeruyVoicePitchPlugin voicePlugin; // onEnable에서 setVoicePlugin으로 주입

    public void setVoicePlugin(DeruyVoicePitchPlugin voicePlugin) {
        this.voicePlugin = voicePlugin;
    }

    /**
     * 해당 플레이어가 현재 투명화 상태(=피치 이펙트 활성 대상)인지 여부.
     */
    public boolean isActive(UUID playerId) {
        return activePlayers.contains(playerId);
    }

    @EventHandler
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getModifiedType() != PotionEffectType.INVISIBILITY) {
            return;
        }

        switch (event.getAction()) {
            case ADDED, CHANGED -> activePlayers.add(player.getUniqueId());
            case CLEARED, REMOVED -> {
                activePlayers.remove(player.getUniqueId());
                // 이펙트가 꺼지면 디코더/인코더/피치 상태를 정리해서 메모리 누수 방지
                if (voicePlugin != null) {
                    voicePlugin.cleanupPlayer(player.getUniqueId());
                }
            }
            default -> {
                // no-op
            }
        }
    }
}
