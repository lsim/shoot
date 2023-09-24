package comms

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.sha1
import java.io.File

class ShootPeer(val name: String, val peer: Peer, private val community: ShootCommunity) {
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
        val bytes: ByteArray = File(path).readBytes()
        // Nonce is the first 8 bytes of the SHA1 hash of the file contents. Hopefully this means we can send multiple versions of the same file
        val nonceBytes = sha1(bytes)
        var nonce = 0L
        for (i in 0..7) {
            val byte = if (nonceBytes.size > i) nonceBytes[i] else 0
            nonce = nonce shl 8
            nonce = nonce or (byte.toLong() and 0xff)
        }
        community.evaSendBinary(peer, ShootCommunity::class.toString(), file.name, bytes, nonce)
    }
}
