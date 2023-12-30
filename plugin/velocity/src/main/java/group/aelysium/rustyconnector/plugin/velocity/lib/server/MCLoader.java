package group.aelysium.rustyconnector.plugin.velocity.lib.server;

import com.sun.jdi.request.DuplicateRequestException;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import group.aelysium.rustyconnector.plugin.velocity.PluginLogger;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.EventDispatch;
import group.aelysium.rustyconnector.plugin.velocity.lib.family.Family;
import group.aelysium.rustyconnector.plugin.velocity.lib.Permission;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.ProxyLang;
import group.aelysium.rustyconnector.plugin.velocity.lib.parties.Party;
import group.aelysium.rustyconnector.plugin.velocity.lib.parties.PartyService;
import group.aelysium.rustyconnector.toolkit.core.log_gate.GateKey;
import group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader.RegisterEvent;
import group.aelysium.rustyconnector.toolkit.velocity.events.mc_loader.UnregisterEvent;
import group.aelysium.rustyconnector.toolkit.velocity.parties.IParty;
import group.aelysium.rustyconnector.toolkit.velocity.players.IPlayer;
import group.aelysium.rustyconnector.toolkit.velocity.server.IMCLoader;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.rmi.ConnectException;
import java.security.InvalidAlgorithmParameterException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MCLoader implements IMCLoader {
    private final UUID uuid;
    private final String displayName;
    private final InetSocketAddress address;
    private RegisteredServer registeredServer = null;
    private Family family;
    private int playerCount = 0;
    private int weight;
    private int softPlayerCap;
    private int hardPlayerCap;
    private AtomicInteger timeout;

    public MCLoader(@NotNull UUID uuid, @NotNull InetSocketAddress address, String displayName, int softPlayerCap, int hardPlayerCap, int weight, int timeout) {
        this.uuid = uuid;
        this.address = address;
        this.displayName = displayName;

        this.weight = Math.max(weight, 0);

        this.softPlayerCap = softPlayerCap;
        this.hardPlayerCap = hardPlayerCap;

        // Soft player cap MUST be at most the same value as hard player cap.
        if(this.softPlayerCap > this.hardPlayerCap) this.softPlayerCap = this.hardPlayerCap;

        this.timeout = new AtomicInteger(timeout);
    }

    public boolean stale() {
        return this.timeout.get() <= 0;
    }

    public void setTimeout(int newTimeout) {
        if(newTimeout < 0) throw new IndexOutOfBoundsException("New timeout must be at least 0!");
        this.timeout.set(newTimeout);
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String uuidOrDisplayName() {
        if(displayName == null) return this.uuid.toString();
        return this.displayName;
    }

    public int decreaseTimeout(int amount) {
        if(amount > 0) amount = amount * -1;
        this.timeout.addAndGet(amount);
        if(this.timeout.get() < 0) this.timeout.set(0);

        return this.timeout.get();
    }

    public String address() {
        return this.serverInfo().getAddress().getHostName() + ":" + this.serverInfo().getAddress().getPort();
    }

    public RegisteredServer registeredServer() {
        if(this.registeredServer == null) throw new IllegalStateException("This server must be registered before you can find its family!");
        return this.registeredServer;
    }

    public ServerInfo serverInfo() { return new ServerInfo(this.uuid.toString(), this.address); }

    /**
     * Set the registered server associated with this PlayerServer.
     * @param registeredServer The RegisteredServer
     * @deprecated This method should never be used in production code! Use `PlayerServer#register` instead! This is only meant for code testing.
     */
    @Deprecated
    public void registeredServer(RegisteredServer registeredServer) {
        this.registeredServer = registeredServer;
    }

    public void register(@NotNull String familyName) throws Exception {
        Tinder api = Tinder.get();
        PluginLogger logger = api.logger();

        try {
            this.family = new Family.Reference(familyName).get();
        } catch (Exception ignore) {
            throw new InvalidAlgorithmParameterException("A family with the id `"+familyName+"` doesn't exist!");
        }

        try {
            if(logger.loggerGate().check(GateKey.REGISTRATION_ATTEMPT))
                ProxyLang.REGISTRATION_REQUEST.send(logger, uuidOrDisplayName(), family.id());

            if(api.services().server().contains(this.uuid)) throw new DuplicateRequestException("Server "+this.uuid+" can't be registered twice!");

            this.registeredServer = api.velocityServer().registerServer(serverInfo());
            if(this.registeredServer == null) throw new NullPointerException("Unable to register the server to the proxy.");

            api.services().server().add(this);
            family.addServer(this);
        } catch (Exception error) {
            if(logger.loggerGate().check(GateKey.REGISTRATION_ATTEMPT))
                ProxyLang.ERROR.send(logger, uuidOrDisplayName(), family.id());
            throw new Exception(error.getMessage());
        }

        EventDispatch.Safe.fireAndForget(new RegisterEvent(family, this));
    }

    public void unregister(boolean removeFromFamily) throws Exception {
        Tinder api = Tinder.get();
        PluginLogger logger = api.logger();
        try {
            MCLoader server = new MCLoader.Reference(this.uuid).get();

            if (logger.loggerGate().check(GateKey.UNREGISTRATION_ATTEMPT))
                ProxyLang.UNREGISTRATION_REQUEST.send(logger, uuidOrDisplayName(), family.id());

            Family family = server.family();

            api.velocityServer().unregisterServer(server.serverInfo());
            api.services().server().remove(this);
            if (removeFromFamily)
                family.removeServer(server);

            EventDispatch.Safe.fireAndForget(new UnregisterEvent(family, server));
        } catch (Exception e) {
            if(logger.loggerGate().check(GateKey.UNREGISTRATION_ATTEMPT))
                ProxyLang.ERROR.send(logger, uuidOrDisplayName(), family.id());
            throw new Exception(e);
        }
    }

    /**
     * Is the server full? Will return `true` if and only if `soft-player-cap` has been reached or surpassed.
     * @return `true` if the server is full
     */
    public boolean full() {
        return this.playerCount >= softPlayerCap;
    }

    /**
     * Is the server maxed out? Will return `true` if and only if `hard-player-cap` has been reached or surpassed.
     * @return `true` if the server is maxed out
     */
    public boolean maxed() {
        return this.playerCount >= hardPlayerCap;
    }


    /**
     * Validates the player against the server's current player count.
     * If the server is full or the player doesn't have permissions to bypass soft and hard player caps. They will be kicked
     * @param player The player to validate
     * @return `true` if the player is able to join. `false` otherwise.
     */
    public boolean validatePlayer(IPlayer player) {
        com.velocitypowered.api.proxy.Player velocityPlayer = player.resolve().orElseThrow();
        if(Permission.validate(
                velocityPlayer,
                "rustyconnector.hardCapBypass",
                Permission.constructNode("rustyconnector.<family id>.hardCapBypass",this.family.id())
        )) return true; // If the player has permission to bypass hard-player-cap, let them in.

        if(this.maxed()) return false; // If the player count is at hard-player-cap. Boot the player.

        if(Permission.validate(
                velocityPlayer,
                "rustyconnector.softCapBypass",
                Permission.constructNode("rustyconnector.<family id>.softCapBypass",this.family.id())
        )) return true; // If the player has permission to bypass soft-player-cap, let them in.

        return !this.full();
    }

    @Override
    public int playerCount() {
        //return 0;
        return this.playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public void playerLeft() {
        if(this.playerCount > 0) this.playerCount -= 1;
    }

    public void playerJoined() {
        this.playerCount += 1;
    }

    public double sortIndex() {
        return this.playerCount;
    }

    @Override
    public int weight() {
        return this.weight;
    }

    @Override
    public int softPlayerCap() {
        return this.softPlayerCap;
    }

    @Override
    public int hardPlayerCap() {
        return this.hardPlayerCap;
    }

    /**
     * Get the family a server is associated with.
     * @return A Family.
     * @throws IllegalStateException If the server hasn't been registered yet.
     * @throws NullPointerException If the family associated with this server doesn't exist.
     */
    public Family family() throws IllegalStateException, NullPointerException {
        if(this.registeredServer == null) throw new IllegalStateException("This server must be registered before you can find its family!");

        Family family;
        try {
            family = new Family.Reference(this.family.id()).get();
        } catch (Exception ignore) {
            throw new NullPointerException("A family with the id `"+this.family.id()+"` doesn't exist!");
        }

        return family;
    }

    public boolean connect(IPlayer player) throws ConnectException {
        try {
            PartyService partyService = Tinder.get().services().party().orElseThrow();
            IParty party = partyService.find(player).orElseThrow();

            try {
                if(partyService.settings().onlyLeaderCanSwitchServers())
                    if(!party.leader().equals(player)) {
                        player.sendMessage(ProxyLang.PARTY_ONLY_LEADER_CAN_SWITCH);
                        return false;
                    }

                party.connect(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ignore) {}

        return directConnect(player);
    }

    public boolean directConnect(IPlayer player) throws ConnectException {
        ConnectionRequestBuilder connection = player.resolve().orElseThrow().createConnectionRequest(this.registeredServer());
        try {
            ConnectionRequestBuilder.Result result = connection.connect().orTimeout(5, TimeUnit.SECONDS).get();

            if(result.isSuccessful()) {
                this.playerJoined();
                return true;
            }
        } catch (Exception e) {
            throw new ConnectException("Unable to connect to that server!", e);
        }

        return false;
    }

    public boolean directConnect(PlayerChooseInitialServerEvent event) {
        try {
            event.setInitialServer(this.registeredServer());
            return true;
        } catch(Exception ignore) {
            return false;
        }
    }

    public void lock() {
        this.family().loadBalancer().lock(this);
    }

    public void unlock() {
        this.family().loadBalancer().unlock(this);
    }

    @Override
    public String toString() {
        return "["+this.serverInfo().getName()+"]" +
               "("+this.serverInfo().getAddress().getHostName()+":"+this.serverInfo().getAddress().getPort()+") - " +
               "["+this.playerCount()+" ("+this.softPlayerCap()+" <> "+this.softPlayerCap()+") w-"+this.weight()+"]" +
               "{"+ this.family.id() +"}";
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCLoader mcLoader = (MCLoader) o;
        return Objects.equals(uuid, mcLoader.uuid());
    }

    public static class Builder {
        private UUID uuid;
        private InetSocketAddress address;
        private String displayName;
        private String podName = "";
        private int weight = 0;
        private int softPlayerCap = 20;
        private int hardPlayerCap = 30;

        protected int initialTimeout = 15;

        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder softPlayerCap(int softPlayerCap) {
            this.softPlayerCap = softPlayerCap;
            return this;
        }

        public Builder hardPlayerCap(int hardPlayerCap) {
            this.hardPlayerCap = hardPlayerCap;
            return this;
        }

        public Builder podName(String podName) {
            this.podName = podName;
            return this;
        }

        public MCLoader build() throws Exception {
            if(this.uuid == null) throw new Exception("uuid can't be null!");
            if(this.address == null) throw new Exception("address can't be null!");

            this.initialTimeout = Tinder.get().services().server().serverTimeout();

            if(this.podName.equals("")) return new MCLoader(uuid, address, displayName, softPlayerCap, hardPlayerCap, weight, initialTimeout);
            return new K8MCLoader(this.podName, uuid, address, displayName, softPlayerCap, hardPlayerCap, weight, initialTimeout);
        }
    }
}
