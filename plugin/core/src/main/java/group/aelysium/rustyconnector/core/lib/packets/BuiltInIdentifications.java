package group.aelysium.rustyconnector.core.lib.packets;

import group.aelysium.rustyconnector.toolkit.core.packet.PacketIdentification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface BuiltInIdentifications {
    /**
     * `MCLoader > Proxy` | MCLoader requesting to interface with Proxy.
     *                    | If the MCLoader is new, it will attempt to be registered.
     *                    | If the MCLoader is already registered, it's connection will refresh.
     *
     *                    | This packet is simultaneously a handshake initializer and a keep-alive packet.
     */
    PacketIdentification MAGICLINK_HANDSHAKE_PING = PacketIdentification.from("RC","MLH");

    /**
     * `Proxy > MCLoader` | Tells the MCLoader it couldn't be registered
     */
    PacketIdentification MAGICLINK_HANDSHAKE_FAIL = PacketIdentification.from("RC","MLHF");

    /**
     * `Proxy > MCLoader` | Tells the MCLoader it was registered and how it should configure itself
     */
    PacketIdentification MAGICLINK_HANDSHAKE_SUCCESS = PacketIdentification.from("RC","MLHS");

    /**
     * `MCLoader > Proxy` | Tells the Proxy to drop the Magic Link between this MCLoader.
     *                    | Typically used when the MCLoader is shutting down so that Magic Link doesn't have to manually scan it.
     */
    PacketIdentification MAGICLINK_HANDSHAKE_DISCONNECT = PacketIdentification.from("RC","MLHK");

    /**
     * `MCLoader > Proxy` | Request to send a player to a family
     */
    PacketIdentification SEND_PLAYER = PacketIdentification.from("RC","SP");

    /**
     * `Proxy > MCLoader` | Queues a tpa on a specific MCLoader. When that player joins the server, they'll be teleported to the other player.
     */
    PacketIdentification QUEUE_TPA = PacketIdentification.from("RC","TPAQP");

    /**
     * `Server > MCLoader` | Tells the proxy to open a server.
     */
    PacketIdentification UNLOCK_SERVER = PacketIdentification.from("RC","US");

    /**
     * `MCLoader > Proxy` | Tells the proxy to close a server.
     */
    PacketIdentification LOCK_SERVER = PacketIdentification.from("RC","LS");

    /**
     * `MCLoader > Proxy` | Tells the proxy to end a game.
     */
    PacketIdentification RANKED_GAME_END = PacketIdentification.from("RC","ERG");

    /**
     * `Proxy > MCLoader` | Tells the MCLoader to start a game.
     */
    PacketIdentification RANKED_GAME_READY = PacketIdentification.from("RC","SRG");

    static List<PacketIdentification> toList() {
        List<PacketIdentification> list = new ArrayList<>();

        list.add(MAGICLINK_HANDSHAKE_PING);
        list.add(MAGICLINK_HANDSHAKE_FAIL);
        list.add(MAGICLINK_HANDSHAKE_DISCONNECT);
        list.add(MAGICLINK_HANDSHAKE_SUCCESS);
        list.add(SEND_PLAYER);
        list.add(QUEUE_TPA);
        list.add(UNLOCK_SERVER);
        list.add(LOCK_SERVER);
        list.add(RANKED_GAME_END);
        list.add(RANKED_GAME_READY);

        return list;
    }

    static PacketIdentification mapping(String id) {
        return toList().stream().filter(entry -> Objects.equals(entry.get(), id)).findFirst().orElseThrow(NullPointerException::new);
    }
}