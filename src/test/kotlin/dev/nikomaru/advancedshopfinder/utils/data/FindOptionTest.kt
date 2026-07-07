package dev.nikomaru.advancedshopfinder.utils.data

import dev.nikomaru.advancedshopfinder.files.PlayerFindOption
import dev.nikomaru.advancedshopfinder.files.server.Config
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class FindOptionTest {
    @Test
    fun generateFindOptionTest() {
        val findOption = PlayerFindOption()
        val json = Json{
            prettyPrint = true
            encodeDefaults = true
        }.encodeToString(findOption)
        println(json)
        assert(true)
    }
}
