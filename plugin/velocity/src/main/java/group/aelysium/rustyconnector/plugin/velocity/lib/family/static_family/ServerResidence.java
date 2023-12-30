package group.aelysium.rustyconnector.plugin.velocity.lib.family.static_family;

import group.aelysium.rustyconnector.toolkit.velocity.family.static_family.IServerResidence;
import group.aelysium.rustyconnector.toolkit.velocity.util.LiquidTimestamp;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.Family;
import group.aelysium.rustyconnector.plugin.velocity.lib.players.Player;
import group.aelysium.rustyconnector.plugin.velocity.lib.server.MCLoader;

public class ServerResidence implements IServerResidence {
    protected Player.Reference player;
    protected MCLoader.Reference server;
    protected Family.Reference family;
    protected Long expiration;

    public ServerResidence(Player.Reference player, MCLoader.Reference server, Family.Reference family, LiquidTimestamp expiration) {
        this.player = player;
        this.server = server;
        this.family = family;

        if(expiration == null) this.expiration = null;
        else this.expiration = expiration.epochFromNow();
    }

    public Player player() {
        return (Player) this.player.get();
    }
    public Player.Reference rawPlayer() {
        return this.player;
    }

    public MCLoader server() {
        return (MCLoader) this.server.get();
    }
    public MCLoader.Reference rawServer() {
        return this.server;
    }

    public Family family() {
        return (Family) this.family.get();
    }

    public Long expiration() {
        return expiration;
    }

    public void expiration(LiquidTimestamp expiration) {
        if(expiration == null) this.expiration = null;
        else this.expiration = expiration.epochFromNow();
    }
}