package me.xginko.villageroptimizer.enums;

public class Permissions {
    public enum Commands {
        VERSION("villageroptimizer.cmd.version"),
        RELOAD("villageroptimizer.cmd.reload"),
        DISABLE("villageroptimizer.cmd.disable"),
        OPTIMIZE_RADIUS("villageroptimizer.cmd.optimize"),
        UNOPTIMIZE_RADIUS("villageroptimizer.cmd.unoptimize");
        private final String permission;
        Commands(String permission) {
            this.permission = permission;
        }
        public String get() {
            return permission;
        }
    }
    public enum Optimize {
        NAMETAG("villageroptimizer.optimize.nametag"),
        BLOCK("villageroptimizer.optimize.block"),
        WORKSTATION("villageroptimizer.optimize.workstation");
        private final String permission;
        Optimize(String permission) {
            this.permission = permission;
        }
        public String get() {
            return permission;
        }
    }
    public enum Bypass {
        TRADE_PREVENTION("villageroptimizer.bypass.tradeprevention"),
        RESTOCK_COOLDOWN("villageroptimizer.bypass.restockcooldown"),
        NAMETAG_COOLDOWN("villageroptimizer.bypass.nametagcooldown"),
        BLOCK_COOLDOWN("villageroptimizer.bypass.blockcooldown"),
        WORKSTATION_COOLDOWN("villageroptimizer.bypass.workstationcooldown"),
        COMMAND_COOLDOWN("villageroptimizer.bypass.commandcooldown");
        private final String permission;
        Bypass(String permission) {
            this.permission = permission;
        }
        public String get() {
            return permission;
        }
    }
}
