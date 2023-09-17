package comms

import ShootPreferences
import comms.messages.GreetingMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.scan
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.messaging.Packet

class ShootCommunity(private val preferences: ShootPreferences) : Community() {
    private val logger = KotlinLogging.logger {}
    override val serviceId = "cc82cb6e-db93-4792-84cf-fd3dbebfef3eef3e"

    private val greetingStream = MutableSharedFlow<ShootPeer>(10)
    val greetingFlow get() = greetingStream
        .scan(emptySet<ShootPeer>()) { acc, latest -> acc.plus(latest) }

    init {
        messageHandlers[GreetingMessage.MESSAGE_ID] = ::handleGreeting
    }

    private fun handleGreeting(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(GreetingMessage.Deserializer)
        greetingStream.tryEmit(ShootPeer(payload.userName, peer.mid))
        logger.info { "Received greeting from ${peer.mid}: ${payload.userName}" }
    }

    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(GreetingMessage.MESSAGE_ID, GreetingMessage(preferences.user))
            send(peer.address, packet)
        }
    }
}
