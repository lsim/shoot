package comms

import ShootPreferences
import comms.messages.GreetingMessageRequest
import comms.messages.GreetingMessageResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload

class ShootCommunity(private val preferences: ShootPreferences) : Community() {
    private val logger = KotlinLogging.logger {}

    override val serviceId = "cc82cb6e-db93-4792-84cf-fd3dbebfef3eef3e"

    private val greetingStream = MutableSharedFlow<ShootPeer>(10)
    val greetingFlow get() = greetingStream
        .scan(emptySet<ShootPeer>()) { acc, latest ->
            if (latest !in acc) {
                logger.info { "New peer: ${latest.peer.mid} acc.size ${acc.size}" }
            } else {
                logger.info { "Known peer: ${latest.peer.mid}" }
            }
            acc.plus(latest)
        }
        .distinctUntilChanged()

    init {
        // IPV8 offers a file transfer protocol - let's enable that
        evaProtocolEnabled = true
        messageHandlers[GreetingMessageRequest.MESSAGE_ID] = ::handleGreetingRequest
        messageHandlers[GreetingMessageResponse.MESSAGE_ID] = ::handleGreetingResponse
    }

    override fun load() {
        super.load()
        evaProtocol?.onReceiveCompleteCallback = { peer, _, _, _,
            ->
            logger.info { "Received file from ${peer.mid}" }
        }
        evaProtocol?.onReceiveProgressCallback = { peer, _, progress,
            ->
            logger.info { "File progress from ${peer.mid}: $progress" }
        }
        evaProtocol?.onSendCompleteCallback = { peer, _, _,
            ->
            logger.info { "Sent file to ${peer.mid}" }
        }
    }

    private fun handleGreetingRequest(packet: Packet) {
        logger.info { "Received greeting request from ${packet.source}" }
        val (peer, payload) = packet.getAuthPayload(GreetingMessageRequest.Deserializer)
        logger.info { "Verified greeting request from ${packet.source}" }
        scope.launch {
            greetingStream.emit(ShootPeer(payload.userName, peer, this@ShootCommunity))
        }
        logger.info { "Received greeting request from peer ${peer.mid}: ${payload.userName}" }
        sendShootGreetingResponse(peer)
    }

    private fun handleGreetingResponse(packet: Packet) {
        logger.info { "Received greeting response from ${packet.source}" }
        val (peer, payload) = packet.getAuthPayload(GreetingMessageResponse.Deserializer)
        logger.info { "Verified greeting response from ${packet.source}" }
        scope.launch {
            greetingStream.emit(ShootPeer(payload.userName, peer, this@ShootCommunity))
        }
        logger.info { "Received greeting response from peer ${peer.mid}: ${payload.userName}" }
    }

    private fun sendShootGreetingRequest(peer: Peer) {
        val packet = serializePacket(GreetingMessageRequest.MESSAGE_ID, GreetingMessageRequest(preferences.user))
        logger.info { "Sending greeting request to ${peer.mid} @ ${peer.address}: ${preferences.user}" }
        send(peer.address, packet)
    }

    private fun sendShootGreetingResponse(peer: Peer) {
        val packet = serializePacket(GreetingMessageResponse.MESSAGE_ID, GreetingMessageResponse(preferences.user))
        logger.info { "Sending greeting response to ${peer.mid} @ ${peer.address}: ${preferences.user}" }
        send(peer.address, packet)
    }

    // For every peer that we are introduced to, send a community-specific greeting
    override fun onIntroductionResponse(
        peer: Peer,
        payload: IntroductionResponsePayload,
    ) {
        super.onIntroductionResponse(peer, payload)
        if (peer.mid !in network.verifiedPeers.map { it.mid }) {
            sendShootGreetingRequest(peer)
        }
    }
}
