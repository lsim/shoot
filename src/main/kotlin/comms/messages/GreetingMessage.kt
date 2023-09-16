package comms.messages

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

data class GreetingMessage(val userName: String) : Serializable {
    override fun serialize(): ByteArray = userName.toByteArray()
    companion object Deserializer : Deserializable<GreetingMessage> {
        const val MESSAGE_ID = 1337_1
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<GreetingMessage, Int> {
            return Pair(GreetingMessage(String(buffer)), buffer.size)
        }
    }
}
