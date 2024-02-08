package me.xginko.villageroptimizer.enums.permissions;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
