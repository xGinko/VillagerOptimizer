package me.xginko.villageroptimizer.enums.permissions;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
