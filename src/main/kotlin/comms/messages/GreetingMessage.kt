package comms.messages

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

// Add to this request/response anything that shoot peers might need to know about each other up front
private val logger = KotlinLogging.logger {}

data class GreetingMessageRequest(val userName: String) : Serializable {
    private val logger = KotlinLogging.logger {}

    override fun serialize(): ByteArray = userName.toByteArray()
    companion object Deserializer : Deserializable<GreetingMessageRequest> {
        const val MESSAGE_ID = ShootMessageIds.GREETING_REQUEST
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<GreetingMessageRequest, Int> {
            return Pair(
                GreetingMessageRequest(String(buffer.sliceArray(IntRange(offset, buffer.size - 1)))),
                buffer.size - offset
            )
        }
    }
}

data class GreetingMessageResponse(val userName: String) : Serializable {
    override fun serialize(): ByteArray = userName.toByteArray()
    companion object Deserializer : Deserializable<GreetingMessageResponse> {
        const val MESSAGE_ID = ShootMessageIds.GREETING_RESPONSE
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<GreetingMessageResponse, Int> {
            return Pair(
                GreetingMessageResponse(String(buffer.sliceArray(IntRange(offset, buffer.size - 1)))),
                buffer.size - offset
            )
        }
    }
}
