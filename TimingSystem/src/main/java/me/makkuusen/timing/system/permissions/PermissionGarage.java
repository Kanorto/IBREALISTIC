package me.makkuusen.timing.system.permissions;

import co.aikar.commands.CommandReplacements;

public enum PermissionGarage implements Permissions {
    USE;

    @Override
    public String getNode() {
        return "timingsystem.garage." + this.toString().replace("_", ".").toLowerCase();
    }

    public static void init(CommandReplacements replacements) {
        for (PermissionGarage perm : values()) {
            Permissions.register(perm, replacements);
        }
    }
}
