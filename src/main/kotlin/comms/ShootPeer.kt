package comms

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import java.io.File
import java.net.URI
import java.nio.file.Paths

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
        community.evaSendBinary(peer, file.name, nonceBytes.toHex(), bytes, nonce)
    }

    fun handleFileReceiveComplete(info: String, fileHash: String, data: ByteArray?) {
        logger.info { "Received file $info with file hash $fileHash" }
        val outputPath = community.preferences["outputPath", community.preferences.defaultOutputPath]
        if (outputPath == "" || data == null) return
        if (sha1(data).toHex() != fileHash) {
            logger.error { "File hash mismatch for $info" }
            return
        }
        val file = File(Paths.get(outputPath, info).toString())
        try {
            file.writeBytes(data)
        } catch (e: Exception) {
            logger.error { "Failed to write file $outputPath: ${e.message}" }
        }
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
