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

class ShootCommunity(val preferences: ShootPreferences) : Community() {
    private val logger = KotlinLogging.logger {}

    override val serviceId = "cc82cb6e-db93-4792-84cf-fd3dbebfef3eef3e"

    private val greetingStream = MutableSharedFlow<ShootPeer>(10)
    val greetingFlow get() = greetingStream
        .scan(emptyMap<String, ShootPeer>()) { acc, latest -> acc.plus(Pair(latest.peer.mid, latest)) }
        .distinctUntilChanged()

    private val greetedPeerIds = mutableSetOf<String>()
    private var shootPeers = mapOf<String, ShootPeer>()

    init {
        // IPV8 offers a file transfer protocol - let's enable that
        evaProtocolEnabled = true
        messageHandlers[GreetingMessageRequest.MESSAGE_ID] = ::handleGreetingRequest
        messageHandlers[GreetingMessageResponse.MESSAGE_ID] = ::handleGreetingResponse
    }

    override fun load() {
        super.load()
        evaProtocol?.onReceiveCompleteCallback = { peer, info, transferId, data,
            ->
            logger.debug { "Received file from ${peer.mid}" }
            shootPeers[peer.mid]?.handleFileReceiveComplete(info, transferId, data)
        }
        evaProtocol?.onReceiveProgressCallback = { peer, state, progress,
            ->
            logger.debug { "File progress from ${peer.mid}: $state, $progress" }
            shootPeers[peer.mid]?.handleFileReceiveProgress(state, progress)
        }
        evaProtocol?.onSendCompleteCallback = { peer, info, nonce,
            ->
            logger.debug { "Sent file to ${peer.mid}" }
            shootPeers[peer.mid]?.handleFileSendComplete(info, nonce)
        }
        scope.launch {
            greetingFlow.collect { peers ->
                shootPeers = peers
            }
        }
    }

    private fun handleGreetingRequest(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(GreetingMessageRequest.Deserializer)
        scope.launch {
            greetingStream.emit(ShootPeer(payload.instanceName, peer, this@ShootCommunity))
        }
        logger.debug { "Received greeting request from peer ${peer.mid}: ${payload.instanceName}" }
        sendShootGreetingResponse(peer)
    }

    private fun handleGreetingResponse(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(GreetingMessageResponse.Deserializer)
        scope.launch {
            greetingStream.emit(ShootPeer(payload.instanceName, peer, this@ShootCommunity))
        }
        logger.debug { "Received greeting response from peer ${peer.mid}: ${payload.instanceName}" }
    }

    private fun instanceId(): String {
        return preferences["instanceId", "${preferences.user}@${preferences.hostname}"]
    }

    private fun sendShootGreetingRequest(peer: Peer) {
        val packet = serializePacket(GreetingMessageRequest.MESSAGE_ID, GreetingMessageRequest(instanceId()))
        logger.debug { "Sending greeting request to ${peer.mid} @ ${peer.address} ${network.getServicesForPeer(peer)}: ${preferences.user}" }
        send(peer.address, packet)
    }

    private fun sendShootGreetingResponse(peer: Peer) {
        val packet = serializePacket(GreetingMessageResponse.MESSAGE_ID, GreetingMessageResponse(instanceId()))
        logger.debug { "Sending greeting response to ${peer.mid} @ ${peer.address}: ${preferences.user}" }
        send(peer.address, packet)
    }

    // For every new shoot peer that we are introduced to, send a community-specific greeting
    override fun onIntroductionResponse(
        peer: Peer,
        payload: IntroductionResponsePayload,
    ) {
        if (serviceId in network.getServicesForPeer(peer) && !greetedPeerIds.contains(peer.mid)) {
            sendShootGreetingRequest(peer)
            greetedPeerIds.add(peer.mid)
        }
        super.onIntroductionResponse(peer, payload)
    }
}
