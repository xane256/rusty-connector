package group.aelysium.rustyconnector.plugin.paper.lib;

import group.aelysium.rustyconnector.core.lib.data_messaging.MessageOrigin;
import group.aelysium.rustyconnector.core.lib.data_messaging.RedisMessage;
import group.aelysium.rustyconnector.core.lib.data_messaging.RedisMessageType;
import group.aelysium.rustyconnector.core.lib.data_messaging.firewall.MessageTunnel;
import group.aelysium.rustyconnector.core.lib.hash.MD5;
import group.aelysium.rustyconnector.core.lib.data_messaging.cache.MessageCache;
import group.aelysium.rustyconnector.core.lib.model.Server;
import group.aelysium.rustyconnector.plugin.paper.PaperRustyConnector;
import group.aelysium.rustyconnector.plugin.paper.lib.config.DefaultConfig;
import group.aelysium.rustyconnector.plugin.paper.lib.database.Redis;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;

public class PaperServer implements Server {
    private MessageCache messageCache;
    private Redis redis;
    private String family;
    private int weight = 0;
    private int softPlayerCap = 20;
    private int hardPlayerCap = 30;
    private final String name;

    private final String privateKey;
    private final InetSocketAddress address;

    public PaperServer(String name, String privateKey, String address, String family, Redis redis) {
        if(name.equals("")) {
            name = MD5.generatePrivateKey(); // Generate a custom string to be the server's name
        }
        this.name = name;
        this.privateKey = privateKey;

        String[] addressSplit = address.split(":");

        this.address = new InetSocketAddress(addressSplit[0], Integer.parseInt(addressSplit[1]));

        this.family = family;

        this.redis = redis;
    }

    public boolean validatePrivateKey(String keyToValidate) {
        return this.privateKey.equals(keyToValidate);
    }

    /**
     * Set the message cache for the proxy. Once this is set it cannot be changed.
     * @param max The max number of messages the cache will accept.
     */
    public void setMessageCache(int max) throws IllegalStateException {
        if(this.messageCache != null) throw new IllegalStateException("This has already been set! You can't set this twice!");

        if(max <= 0) max = 0;
        if(max > 500) max = 500;
        this.messageCache = new MessageCache(max);
    }

    public MessageCache getMessageCache() {
        return this.messageCache;
    }

    public InetSocketAddress getAddress() { return this.address; }

    public String getFamily() { return this.family; }

    @Override
    public int getPlayerCount() {
        return PaperRustyConnector.getInstance().getServer().getOnlinePlayers().size();
    }

    @Override
    public int getSortIndex() {
        return 0;
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public int getSoftPlayerCap() {
        return this.softPlayerCap;
    }

    @Override
    public int getHardPlayerCap() {
        return this.hardPlayerCap;
    }

    public void killRedis() {
        this.redis.disconnect();
    }

    /**
     * Set the player cap for this server. If soft cap is larger than hard cap. Set soft cap to be the same value as hard cap.
     * @param softPlayerCap The soft player cap
     * @param hardPlayerCap The hard player cap
     */
    public void setPlayerCap(int softPlayerCap, int hardPlayerCap) {

        PaperRustyConnector.getInstance().getServer().setMaxPlayers(hardPlayerCap);

        if(softPlayerCap >= hardPlayerCap) {
            this.hardPlayerCap = hardPlayerCap;
            this.softPlayerCap = hardPlayerCap;
            PaperRustyConnector.getInstance().logger().log("soft-player-cap was set to be larger than hard-player-cap. Running in `player-limit` mode.");
            return;
        }
        this.hardPlayerCap = hardPlayerCap;
        this.softPlayerCap = softPlayerCap;
    }

    public void registerToProxy() {
        RedisMessage registrationMessage = new RedisMessage(
                this.privateKey,
                RedisMessageType.REG,
                this.address.getHostName()+":"+this.address.getPort()
        );
        registrationMessage.addParameter("family",this.family);
        registrationMessage.addParameter("name",this.name);
        registrationMessage.addParameter("soft-cap", String.valueOf(this.softPlayerCap));
        registrationMessage.addParameter("hard-cap", String.valueOf(this.hardPlayerCap));
        registrationMessage.addParameter("weight", String.valueOf(this.weight));

        registrationMessage.dispatchMessage(this.redis);
    }

    public void unregisterFromProxy() {
        RedisMessage registrationMessage = new RedisMessage(
                this.privateKey,
                RedisMessageType.UNREG,
                this.address.getHostName()+":"+this.address.getPort()
        );
        registrationMessage.addParameter("family",this.family);
        registrationMessage.addParameter("name",this.name);

        registrationMessage.dispatchMessage(this.redis);
    }

    @Deprecated
    public void sendMessage(String rawMessage) {
        RedisMessage.create(rawMessage, MessageOrigin.SERVER, this.getAddress()).dispatchMessage(this.redis);
    }

    /**
     * Requests that the proxy moves this player to another server.
     * @param player The player to send.
     * @param familyName The name of the family to send to.
     */
    public void sendToOtherFamily(Player player, String familyName) {
        RedisMessage registrationMessage = new RedisMessage(
                this.privateKey,
                RedisMessageType.SEND,
                this.address.getHostName()+":"+this.address.getPort()
        );
        registrationMessage.addParameter("uuid",player.getUniqueId().toString());
        registrationMessage.addParameter("family",familyName);

        registrationMessage.dispatchMessage(this.redis);
    }

    /**
     * Sends a pong message to the proxy.
     */
    public void pong() {
        RedisMessage registrationMessage = new RedisMessage(
                this.privateKey,
                RedisMessageType.PONG,
                this.address.getHostName()+":"+this.address.getPort()
        );
        registrationMessage.addParameter("name", this.name);
        registrationMessage.addParameter("player-count", String.valueOf(this.getPlayerCount()));

        registrationMessage.dispatchMessage(this.redis);
    }

    public static PaperServer init(DefaultConfig config) throws IllegalAccessException {
        PaperRustyConnector plugin = PaperRustyConnector.getInstance();

        plugin.logger().log("Preparing Redis...");

        Redis redis = new Redis();
        redis.setConnection(
                config.getRedis_host(),
                config.getRedis_port(),
                config.getRedis_password(),
                config.getRedis_dataChannel()
        );
        redis.connect(plugin);

        PaperServer server = new PaperServer(
                config.getServer_name(),
                config.getPrivate_key(),
                config.getServer_address(),
                config.getServer_family(),
                redis
        );
        server.setMessageCache(50);

        server.setPlayerCap(
                config.getServer_playerCap_soft(),
                config.getServer_playerCap_hard()
        );

        return server;
    }
}
