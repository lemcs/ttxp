package com.lemc.ttxp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.event.Listener;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

import java.util.ArrayList;
import java.util.List;
public class Ttxp extends JavaPlugin implements Listener {

    private ScoreboardManager manager;
    private Scoreboard scoreboard;
    private Objective objective;
    private int countdown = -1; // 游戏开始倒计时未开始
    private int staus = 0; // 游戏开始前的倒计时未开始
    private int vipquicks = 0;
    private boolean isFinalCountdown = false; // 标记是否进入最后15秒阶段
    private List<Player> finishers = new ArrayList<>(); // 到达终点的玩家列表

    @Override
    public void onEnable() {
        getLogger().info("弹跳下坡 启动成功！");
        VelocityHook.setup(this);
        getServer().getPluginManager().registerEvents(this, this);
        setupScoreboard();
        startMian();   // 自动检测在线玩家，控制倒计时逻辑
        this.getCommand("start").setExecutor(new setUseVip());

    }
    @Override
    public void onDisable() {
        getLogger().info("Ttxp 已关闭！");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            event.setCancelled(true);
        }
    }

    private void setupScoreboard() {
        manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        objective = scoreboard.registerNewObjective("Timer", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.GOLD + "LeMC弹跳下坡");

        updateScoreboard(0, "人数不足2人");
    }

    private void updateScoreboard(int countdown, String status) {
        scoreboard.getEntries().forEach(scoreboard::resetScores); // 清除旧数据

        // 显示倒计时和在线人数
        objective.getScore(ChatColor.YELLOW + "在线人数: " + Bukkit.getOnlinePlayers().size() + "/16").setScore(Bukkit.getOnlinePlayers().size());
        objective.getScore(ChatColor.GREEN + "剩余:").setScore(countdown);
        objective.getScore(ChatColor.AQUA + "状态: " + status).setScore(0);
        objective.getScore(ChatColor.AQUA + "网站:wdsj.bf").setScore(0);

        // 为每个玩家更新记分板
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private  void  startMian(){
        new BukkitRunnable() {
            @Override
            public void run() {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                if(onlinePlayers >=2){
                    if(staus == 0){
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.sendTitle(ChatColor.YELLOW + "游戏将会在60秒后开始", "", 10, 70, 20);
                        });
                        staus = 1;
                        countdown = 60;
                    }
                    if(staus == 1){
                        countdown--;
                        if(countdown == 0){
                            countdown = 120;
                            staus = 2;
                            CommandSender console = Bukkit.getConsoleSender();
                            // 执行命令
                            Bukkit.dispatchCommand(console, "fill 22 131 -202 11 130 -202 air");
                        }
                        if(3 >= countdown){
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                player.sendTitle(ChatColor.YELLOW + String.valueOf(countdown), "", 10, 70, 20);
                            });
                        }
                    }
                }
                if(onlinePlayers ==16 || vipquicks == 1){
                    if(staus == 1){
                        if(countdown >= 15){
                            countdown = 15;
                        }

                    }
                }
                if(2>onlinePlayers){
                    if(staus == 1){
                        countdown = -1;
                        staus = 0;
                    }
                }
                if(staus == 2){
                    countdown--;
                    updateScoreboard(countdown, "游戏开始");
                    if(3 >= countdown){
                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.sendTitle(ChatColor.YELLOW + String.valueOf(countdown), "", 10, 70, 20);
                        });
                    }
                    if(countdown == 0){
                        endGame();
                    }
                }
                if(staus == 0){
                    updateScoreboard(0, "人数不足2人");
                }
                if(staus == 1){
                    updateScoreboard(countdown, "游戏即将开始");
                }

            }
        }.runTaskTimer(this, 0, 20); // 每秒检测一次在线人数
    }

    private void endGame() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.sendTitle(ChatColor.YELLOW + "游戏结束！", "", 10, 70, 20);
            VelocityHook.connectToHub(player); // 通过 Velocity 跨服传送到 Lobby
        });
        Bukkit.broadcastMessage(ChatColor.RED + "游戏结束！服务器即将关闭...");
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.shutdown(); // 关闭服务器
            }
        }.runTaskLater(this, 100); // 延迟 5 秒后关闭服务器
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // 检测玩家是否已经完成
        if (finishers.contains(player)) return;

        // 获取玩家脚下的方块
        Block block = player.getLocation().subtract(0, 1, 0).getBlock();

        // 如果玩家踩到羊毛
        if (block.getType() == Material.WOOL) {
            finishers.add(player); // 添加到完成列表
            player.setGameMode(GameMode.SPECTATOR); // 切换为旁观者模式

            int rank = finishers.size(); // 玩家排名
            player.sendTitle(ChatColor.GOLD + "到达终点", ChatColor.GREEN + "您是第 " + rank + " 名！", 10, 70, 20);

            Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " 到达终点，排名：" + rank);
        }
    }

    private class setUseVip implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (label.equalsIgnoreCase("start")) {
                vipquicks = 1;
                return true;
            }
            return false; // 未处理的命令
        }
    }
}
