package me.xginko.villageroptimizer.struct.enums;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public final class Permissions {

    public enum Bypass {
        TRADE_PREVENTION(new Permission("villageroptimizer.bypass.tradeprevention",
                "Permission to bypass unoptimized trade prevention", PermissionDefault.FALSE)),
        RESTOCK_COOLDOWN(new Permission("villageroptimizer.bypass.restockcooldown",
                "Permission to bypass restock cooldown on optimized villagers", PermissionDefault.FALSE)),
        NAMETAG_COOLDOWN(new Permission("villageroptimizer.bypass.nametagcooldown",
                "Permission to bypass Nametag optimization cooldown", PermissionDefault.FALSE)),
        BLOCK_COOLDOWN(new Permission("villageroptimizer.bypass.blockcooldown",
                "Permission to bypass Block optimization cooldown", PermissionDefault.FALSE)),
        WORKSTATION_COOLDOWN(new Permission("villageroptimizer.bypass.workstationcooldown",
                "Permission to bypass Workstation optimization cooldown", PermissionDefault.FALSE)),
        COMMAND_COOLDOWN(new Permission("villageroptimizer.bypass.commandcooldown",
                "Permission to bypass command optimization cooldown", PermissionDefault.FALSE));

        private final Permission permission;

        Bypass(Permission permission) {
            this.permission = permission;
        }

        public Permission get() {
            return permission;
        }
    }

    public enum Commands {
        VERSION(new Permission("villageroptimizer.cmd.version",
                "Permission get the plugin version", PermissionDefault.OP)),
        RELOAD(new Permission("villageroptimizer.cmd.reload",
                "Permission to reload the plugin config", PermissionDefault.OP)),
        DISABLE(new Permission("villageroptimizer.cmd.disable",
                "Permission to disable the plugin", PermissionDefault.OP)),
        OPTIMIZE_RADIUS(new Permission("villageroptimizer.cmd.optimize",
                "Permission to optimize villagers in a radius", PermissionDefault.TRUE)),
        UNOPTIMIZE_RADIUS(new Permission("villageroptimizer.cmd.unoptimize",
                "Permission to unoptimize villagers in a radius", PermissionDefault.TRUE));

        private final Permission permission;

        Commands(Permission permission) {
            this.permission = permission;
        }

        public Permission get() {
            return permission;
        }
    }

    public enum Optimize {
        NAMETAG(new Permission("villageroptimizer.optimize.nametag",
                "Permission to optimize / unoptimize using Nametags", PermissionDefault.TRUE)),
        BLOCK(new Permission("villageroptimizer.optimize.block",
                "Permission to optimize / unoptimize using Blocks", PermissionDefault.TRUE)),
        WORKSTATION(new Permission("villageroptimizer.optimize.workstation",
                "Permission to optimize / unoptimize using Workstations", PermissionDefault.TRUE));

        private final Permission permission;

        Optimize(Permission permission) {
            this.permission = permission;
        }

        public Permission get() {
            return permission;
        }
    }

    public static void registerAll() {
        for (Bypass perm : Bypass.values()) {
            try {
                Bukkit.getPluginManager().addPermission(perm.get());
            } catch (IllegalArgumentException ignored) {}
        }

        for (Commands perm : Commands.values()) {
            try {
                Bukkit.getPluginManager().addPermission(perm.get());
            } catch (IllegalArgumentException ignored) {}
        }

        for (Optimize perm : Optimize.values()) {
            try {
                Bukkit.getPluginManager().addPermission(perm.get());
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
