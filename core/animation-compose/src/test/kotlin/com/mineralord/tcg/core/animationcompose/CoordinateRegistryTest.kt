package com.mineralord.tcg.core.animationcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoordinateRegistryTest {

    private fun bounds(x: Float, y: Float, w: Int = 10, h: Int = 20) =
        SlotBounds(positionInRoot = Offset(x, y), size = IntSize(w, h))

    @Test
    fun `registro y consulta`() {
        val registry = CoordinateRegistry()
        val id = SlotId("active")
        assertNull(registry.bounds(id))

        registry.place(id, bounds(1f, 2f))

        assertEquals(bounds(1f, 2f), registry.bounds(id))
        assertTrue(registry.contains(id))
        assertEquals(1, registry.size)
    }

    @Test
    fun `actualizacion sobrescribe (la ultima gana)`() {
        val registry = CoordinateRegistry()
        val id = SlotId("bench-1")

        registry.place(id, bounds(0f, 0f))
        registry.place(id, bounds(5f, 5f))

        assertEquals(bounds(5f, 5f), registry.bounds(id))
        assertEquals(1, registry.size) // sigue siendo una sola ranura
    }

    @Test
    fun `eliminacion`() {
        val registry = CoordinateRegistry()
        val id = SlotId("gone")
        registry.place(id, bounds(1f, 1f))

        registry.remove(id)

        assertNull(registry.bounds(id))
        assertFalse(registry.contains(id))
        assertEquals(0, registry.size)
    }

    @Test
    fun `consulta de id inexistente devuelve null`() {
        assertNull(CoordinateRegistry().bounds(SlotId("nope")))
    }

    @Test
    fun `multiples elementos independientes`() {
        val registry = CoordinateRegistry()
        val a = SlotId("a")
        val b = SlotId("b")
        val c = SlotId("c")

        registry.place(a, bounds(1f, 1f))
        registry.place(b, bounds(2f, 2f))
        registry.place(c, bounds(3f, 3f))

        assertEquals(3, registry.size)
        assertEquals(bounds(1f, 1f), registry.bounds(a))
        assertEquals(bounds(2f, 2f), registry.bounds(b))
        assertEquals(bounds(3f, 3f), registry.bounds(c))

        registry.remove(b)
        assertEquals(2, registry.size)
        assertNull(registry.bounds(b))
        assertEquals(bounds(1f, 1f), registry.bounds(a)) // los demás intactos
    }

    @Test
    fun `consistencia ante multiples actualizaciones`() {
        val registry = CoordinateRegistry()
        val id = SlotId("moving")

        repeat(100) { i -> registry.place(id, bounds(i.toFloat(), i.toFloat())) }

        assertEquals(bounds(99f, 99f), registry.bounds(id))
        assertEquals(1, registry.size)
    }

    @Test
    fun `clear vacia el registro`() {
        val registry = CoordinateRegistry()
        repeat(5) { registry.place(SlotId("s$it"), bounds(it.toFloat(), 0f)) }
        assertEquals(5, registry.size)

        registry.clear()

        assertEquals(0, registry.size)
    }

    @Test
    fun `SlotBounds expone centro y rect coherentes`() {
        val b = SlotBounds(positionInRoot = Offset(10f, 20f), size = IntSize(100, 40))
        assertEquals(Offset(60f, 40f), b.center)
        val rect = b.toRect()
        assertEquals(10f, rect.left)
        assertEquals(20f, rect.top)
        assertEquals(110f, rect.right)
        assertEquals(60f, rect.bottom)
    }
}
