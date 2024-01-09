package me.xginko.villageroptimizer.enums.permissions;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
