package me.xginko.villageroptimizer.enums;

public class Permissions {
    public enum Commands {
        VERSION("villageroptimizer.cmd.version"),
        RELOAD("villageroptimizer.cmd.reload"),
        OPTIMIZE_RADIUS("villageroptimizer.cmd.optimize"),
        UNOPTIMIZE_RADIUS("villageroptimizer.cmd.unoptimize");
        private final String key;
        Commands(String key) {
            this.key = key;
        }
        public String get() {
            return key;
        }
    }
    public enum Optimize {
        NAMETAG("villageroptimizer.optimize.nametag"),
        BLOCK("villageroptimizer.optimize.block"),
        WORKSTATION("villageroptimizer.optimize.workstation");
        private final String key;
        Optimize(String key) {
            this.key = key;
        }
        public String get() {
            return key;
        }
    }
    public enum Bypass {
        TRADE_PREVENTION("villageroptimizer.bypass.tradeprevention"),
        RESTOCK_COOLDOWN("villageroptimizer.bypass.restockcooldown"),
        NAMETAG_COOLDOWN("villageroptimizer.bypass.nametagcooldown"),
        BLOCK_COOLDOWN("villageroptimizer.bypass.blockcooldown"),
        WORKSTATION_COOLDOWN("villageroptimizer.bypass.workstationcooldown"),
        COMMAND_COOLDOWN("villageroptimizer.bypass.commandcooldown");
        private final String key;
        Bypass(String key) {
            this.key = key;
        }
        public String get() {
            return key;
        }
    }
}
