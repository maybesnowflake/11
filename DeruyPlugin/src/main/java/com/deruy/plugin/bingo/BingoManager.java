package com.deruy.plugin.bingo;

import com.deruy.plugin.DeruyPlugin;
import com.deruy.plugin.events.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 빙고 이벤트: 5x5(사이즈 config화) 랜덤 빙고판을 "전체 바이옴+아이템 풀 - 제외리스트"로 생성.
 * 팀별로 각 칸 완료여부를 추적하고, 설정된 줄 수만큼 완성하면 승리.
 *
 * config:
 *   bingo.size                 : 보드 한 변 크기 (기본 5)
 *   bingo.lines-required       : 승리에 필요한 줄 수 (기본 1)
 *   bingo.excluded-biomes      : 제외할 바이옴 이름 목록
 *   bingo.excluded-items       : 제외할 아이템 Material 이름 목록
 */
public class BingoManager implements GameEvent {

    private final DeruyPlugin plugin;
    private boolean running = false;

    private int size;
    private int linesRequired;
    private List<BingoCell> board = new ArrayList<>();

    // 팀 이름 -> 완료된 칸 인덱스 집합
    private final Map<String, Set<Integer>> teamProgress = new HashMap<>();
    // 플레이어 -> 팀 이름 (간단한 내부 팀 배정)
    private final Map<String, String> playerTeams = new HashMap<>();
    // 이미 빙고를 달성한 팀 (중복 알림 방지)
    private final Set<String> wonTeams = new HashSet<>();

    public BingoManager(DeruyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "bingo";
    }

    // ---------------- 팀 배정 ----------------

    public void setTeam(Player player, String team) {
        playerTeams.put(player.getUniqueId().toString(), team);
        teamProgress.computeIfAbsent(team, t -> new HashSet<>());
    }

    public String getTeam(Player player) {
        return playerTeams.get(player.getUniqueId().toString());
    }

    // ---------------- 보드 생성 ----------------

    private List<BingoCell> buildPool() {
        List<String> excludedBiomes = plugin.getConfig().getStringList("bingo.excluded-biomes");
        List<String> excludedItems = plugin.getConfig().getStringList("bingo.excluded-items");

        List<BingoCell> pool = new ArrayList<>();

        for (Biome biome : Biome.values()) {
            String name = biome.getKey().getKey().toUpperCase();
            if (excludedBiomes.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
                pool.add(new BingoCell(BingoCell.Type.BIOME, name));
            }
        }

        for (Material material : Material.values()) {
            if (!material.isItem() || material.isLegacy() || material.isAir()) continue;
            String name = material.name();
            if (excludedItems.stream().noneMatch(n -> n.equalsIgnoreCase(name))) {
                pool.add(new BingoCell(BingoCell.Type.ITEM, name));
            }
        }

        return pool;
    }

    @Override
    public void start() {
        if (running) return;

        size = plugin.getConfig().getInt("bingo.size", 5);
        linesRequired = plugin.getConfig().getInt("bingo.lines-required", 1);

        List<BingoCell> pool = buildPool();
        Collections.shuffle(pool);

        int needed = size * size;
        if (pool.size() < needed) {
            plugin.getLogger().warning("빙고 풀이 부족합니다 (필요 " + needed + ", 보유 " + pool.size() + ")");
            return;
        }

        board = new ArrayList<>(pool.subList(0, needed));
        teamProgress.clear();
        wonTeams.clear();
        for (String team : new HashSet<>(playerTeams.values())) {
            teamProgress.put(team, new HashSet<>());
        }

        running = true;
        Bukkit.broadcastMessage("§d§l빙고 §e이벤트가 시작되었습니다! (" + size + "x" + size + ", " + linesRequired + "줄 빙고)");
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        board.clear();
        teamProgress.clear();
        wonTeams.clear();
        Bukkit.broadcastMessage("§d§l빙고 §e이벤트가 종료되었습니다.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // ---------------- 진행 체크 (리스너에서 호출) ----------------

    public void checkBiome(Player player, Biome biome) {
        if (!running) return;
        String team = getTeam(player);
        if (team == null) return;

        String name = biome.getKey().getKey().toUpperCase();
        markIfMatch(team, player, BingoCell.Type.BIOME, name);
    }

    public void checkItem(Player player, Material material) {
        if (!running) return;
        String team = getTeam(player);
        if (team == null) return;

        markIfMatch(team, player, BingoCell.Type.ITEM, material.name());
    }

    private void markIfMatch(String team, Player player, BingoCell.Type type, String value) {
        if (wonTeams.contains(team)) return;

        for (int i = 0; i < board.size(); i++) {
            BingoCell cell = board.get(i);
            if (cell.getType() != type || !cell.getValue().equalsIgnoreCase(value)) continue;

            Set<Integer> progress = teamProgress.computeIfAbsent(team, t -> new HashSet<>());
            if (progress.add(i)) {
                Bukkit.broadcastMessage("§d[빙고] §e팀 '" + team + "'§7(" + player.getName() + ")§e가 칸을 완료했습니다: " + cell.display());
                checkLines(team);
            }
        }
    }

    private void checkLines(String team) {
        Set<Integer> progress = teamProgress.get(team);
        if (progress == null) return;

        int completedLines = 0;

        // 가로
        for (int r = 0; r < size; r++) {
            boolean full = true;
            for (int c = 0; c < size; c++) {
                if (!progress.contains(r * size + c)) { full = false; break; }
            }
            if (full) completedLines++;
        }
        // 세로
        for (int c = 0; c < size; c++) {
            boolean full = true;
            for (int r = 0; r < size; r++) {
                if (!progress.contains(r * size + c)) { full = false; break; }
            }
            if (full) completedLines++;
        }
        // 대각선
        boolean diag1 = true, diag2 = true;
        for (int i = 0; i < size; i++) {
            if (!progress.contains(i * size + i)) diag1 = false;
            if (!progress.contains(i * size + (size - 1 - i))) diag2 = false;
        }
        if (diag1) completedLines++;
        if (diag2) completedLines++;

        if (completedLines >= linesRequired && wonTeams.add(team)) {
            Bukkit.broadcastMessage("§d§l[빙고] §a§l팀 '" + team + "'이(가) " + linesRequired + "줄 빙고를 달성했습니다! 승리!");
            // TODO: 보상 지급 로직 연결
        }
    }

    public List<BingoCell> getBoard() {
        return board;
    }

    public int getSize() {
        return size;
    }
}
