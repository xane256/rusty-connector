package group.aelysium.rustyconnector.toolkit.velocity.events.player;

import group.aelysium.rustyconnector.toolkit.core.events.Cancelable;
import group.aelysium.rustyconnector.toolkit.velocity.family.IFamily;
import group.aelysium.rustyconnector.toolkit.velocity.load_balancing.ILoadBalancer;
import group.aelysium.rustyconnector.toolkit.velocity.players.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;

/**
 * Represents a player successfully connecting to a family.
 */
public class FamilyPostJoinEvent extends Cancelable {
    protected final IFamily family;
    protected final IMCLoader mcLoader;
    protected final IPlayer player;

    public FamilyPostJoinEvent(IFamily family, IMCLoader mcLoader, IPlayer player) {
        this.family = family;
        this.mcLoader = mcLoader;
        this.player = player;
    }

    public IFamily family() {
        return family;
    }
    public IMCLoader mcLoader() {
        return mcLoader;
    }
    public IPlayer player() {
        return player;
    }
}