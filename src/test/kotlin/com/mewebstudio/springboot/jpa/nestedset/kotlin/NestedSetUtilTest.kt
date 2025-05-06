package com.mewebstudio.springboot.jpa.nestedset.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@DisplayName("Test for NestedSetUtil class.")
class NestedSetUtilTest {
    private lateinit var root: TestNode
    private lateinit var child1: TestNode
    private lateinit var child2: TestNode

    private lateinit var rootResponse: INestedSetNodeResponse<Int>
    private lateinit var child1Response: INestedSetNodeResponse<Int>
    private lateinit var child2Response: INestedSetNodeResponse<Int>

    @BeforeEach
    fun setUp() {
        // Mock nodes
        root = mock<TestNode>()
        child1 = mock<TestNode>()
        child2 = mock<TestNode>()

        `when`(root.id).thenReturn(1)
        `when`(root.parent).thenReturn(null)
        `when`(root.left).thenReturn(1)

        `when`(child1.id).thenReturn(2)
        `when`(child1.parent).thenReturn(root)
        `when`(child1.left).thenReturn(2)

        `when`(child2.id).thenReturn(3)
        `when`(child2.parent).thenReturn(root)
        `when`(child2.left).thenReturn(3)

        // Mock response objects
        rootResponse = mock()
        child1Response = mock()
        child2Response = mock()
    }

    @Test
    fun testTreeBuildsCorrectHierarchy() {
        val nodes = listOf(root, child1, child2)

        // Response mapping
        val converter: (TestNode) -> INestedSetNodeResponse<Int> = { node ->
            when (node.id) {
                1 -> rootResponse
                2 -> child1Response
                else -> child2Response
            }
        }

        `when`(rootResponse.withChildren(anyList())).thenReturn(rootResponse)
        `when`(child1Response.withChildren(anyList())).thenReturn(child1Response)
        `when`(child2Response.withChildren(anyList())).thenReturn(child2Response)

        val result = NestedSetUtil.tree(nodes, converter)

        assertEquals(1, result.size)
        assertSame(rootResponse, result[0])

        verify(rootResponse).withChildren(listOf(child1Response, child2Response))
        verify(child1Response).withChildren(emptyList())
        verify(child2Response).withChildren(emptyList())
    }

    @Test
    fun testTreeReturnsEmptyListIfNodesIsEmpty() {
        val result: List<INestedSetNodeResponse<Int>> = NestedSetUtil.tree(emptyList()) { rootResponse }
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
}

class TestNode(
    override val id: Int,
    override var left: Int,
    override var right: Int,
    override var parent: TestNode? = null
) : INestedSetNode<Int, TestNode>
