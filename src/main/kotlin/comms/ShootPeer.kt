package comms

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.ipv8.util.sha1
import java.io.File
import java.net.URI

class ShootPeer(val name: String, val peer: Peer, private val community: ShootCommunity) {
    private val logger = KotlinLogging.logger {}

    override fun toString(): String {
        return "ShootPeer(name='$name', id='$peer')"
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ShootPeer -> peer.mid == other.peer.mid
            else -> false
        }
    }

    override fun hashCode(): Int {
        return peer.mid.hashCode()
    }

    fun sendFile(path: String) {
        val file = File(path)
        var bytes = ByteArray(0)
        try {
            bytes = File(URI(path)).readBytes()
        } catch (e: Exception) {
            logger.error { "Failed to read file $path bound for ${peer.mid}: ${e.message}" }
        }
        logger.debug { "Sending bytes $${bytes.size}" }
        // Nonce is the first 8 bytes of the SHA1 hash of the file contents. Hopefully this means we can send multiple versions of the same file
        val nonceBytes = sha1(bytes)
        var nonce = 0L
        for (i in 0..7) {
            val byte = if (nonceBytes.size > i) nonceBytes[i] else 0
            nonce = nonce shl 8
            nonce = nonce or (byte.toLong() and 0xff)
        }
        logger.debug { "Sending file ${file.name} to ${peer.mid} with nonce $nonce" }
        community.evaSendBinary(peer, ShootCommunity::class.toString(), file.name, bytes, nonce)
    }

    fun handleFileReceiveComplete(info: String, transferId: String, data: ByteArray?) {
        logger.info { "Received file $info with transferId $transferId" }
//        TODO("Not yet implemented")
    }

    fun handleFileReceiveProgress(state: String, progress: TransferProgress) {
        logger.info { "Received file progress $state, $progress" }
//        TODO("Not yet implemented")
    }

    fun handleFileSendComplete(info: String, nonce: ULong) {
        logger.info { "Sent file $info with nonce $nonce" }
//        TODO("Not yet implemented")
    }
}
