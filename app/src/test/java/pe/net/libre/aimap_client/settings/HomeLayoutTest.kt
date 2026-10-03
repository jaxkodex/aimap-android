package pe.net.libre.aimap_client.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun a_stored_name_maps_back_to_its_layout() {
        HomeLayout.entries.forEach { assertEquals(it, homeLayoutOf(it.name)) }
    }

    @Test
    fun nothing_stored_or_nothing_known_means_v4() {
        assertEquals(HomeLayout.V4, homeLayoutOf(null))
        assertEquals(HomeLayout.V4, homeLayoutOf(""))
        assertEquals(HomeLayout.V4, homeLayoutOf("v6"))
        assertEquals(HomeLayout.V4, homeLayoutOf("v4"))
    }

    @Test
    fun the_choices_read_as_v4_and_v5() {
        assertEquals(listOf("v4", "v5"), HomeLayout.entries.map { it.label })
    }
}
