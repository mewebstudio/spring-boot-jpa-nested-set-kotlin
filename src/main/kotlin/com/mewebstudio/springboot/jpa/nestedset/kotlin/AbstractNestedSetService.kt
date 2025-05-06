package com.mewebstudio.springboot.jpa.nestedset.kotlin

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional

/**
 * Abstract service class for managing nested set trees.
 *
 * @param T The type of the nested set node.
 * @param ID The type of the identifier for the nested set node.
 */
abstract class AbstractNestedSetService<T : INestedSetNode<ID, T>, ID>(
    open val repository: JpaNestedSetRepository<T, ID>
) {
    /**
     * Companion object containing constants.
     */
    companion object {
        private const val TEMP_OFFSET = Int.MAX_VALUE
    }

    /**
     * Get ancestors of a node.
     *
     * @param entity The node whose ancestors are to be found.
     * @return A list of ancestor nodes.
     */
    fun getAncestors(entity: T): List<T> = repository.findAncestors(entity.left, entity.right)

    /**
     * Get descendants of a node.
     *
     * @param entity The node whose descendants are to be found.
     * @return A list of descendant nodes.
     */
    fun getDescendants(entity: T): List<T> = repository.findDescendants(entity.left, entity.right)

    /**
     * Move a node up in the tree.
     *
     * @param node The node to be moved up.
     * @return The updated node.
     */
    @Transactional
    open fun moveUp(node: T): T = moveNode(node, MoveNodeDirection.UP)

    /**
     * Move a node down in the tree.
     *
     * @param node The node to be moved down.
     * @return The updated node.
     */
    @Transactional
    open fun moveDown(node: T): T = moveNode(node, MoveNodeDirection.DOWN)

    /**
     * Creates a new node in the nested set tree.
     *
     * @param allNodes The list of all nodes in the tree.
     * @param node T The new node to be created.
     * @return T The created node.
     */
    @Transactional
    protected open fun createNode(allNodes: List<T>, node: T): T = run {
        val gap: Pair<Int, Int> = getNodeGap(allNodes, node.parent)

        repository.save(node.apply {
            left = gap.first
            right = gap.second
        })
    }

    /**
     * Creates a new node in the nested set tree.
     *
     * @param node T The new node to be created.
     * @return T The created node.
     */
    @Transactional
    protected open fun createNode(node: T): T = createNode(repository.findAllOrderedByLeft(), node)

    /**
     * Get the gap for inserting a new node in the nested set tree.
     *
     * @param allNodes The list of all nodes in the tree.
     * @param parent T? The parent node under which the new node will be created.
     * @return A pair of integers representing the left and right values for the new node.
     */
    @Transactional
    protected open fun getNodeGap(allNodes: List<T>, parent: T?): Pair<Int, Int> {
        return if (parent == null) {
            val maxRight = allNodes.maxOfOrNull { it.right } ?: 0
            Pair(maxRight + 1, maxRight + 2)
        } else {
            val parentId = parent.id
            val parentNode = repository.lockNode(parentId)
                ?: throw EntityNotFoundException("Parent not found: $parentId")

            val insertAt = parentNode.right
            val shiftedNodes = repository.findNodesToShift(insertAt).onEach { node ->
                if (node.left >= insertAt) node.left += 2
                if (node.right >= insertAt) node.right += 2
            }

            parentNode.right += 2
            saveAllNodes(listOf(parentNode) + shiftedNodes)
            Pair(insertAt, insertAt + 1)
        }
    }

    /**
     * Update a node in the nested set tree.
     *
     * @param node T The node to be updated.
     * @param newParent T? The new parent node under which the node will be moved.
     * @return T The updated node.
     */
    @Transactional
    protected open fun updateNode(node: T, newParent: T?): T = run {
        // Handle parent change if parentId is provided
        newParent?.id.let {
            // Check for cyclic reference and move category
            if (newParent != null && isDescendant(node, newParent)) {
                throw IllegalArgumentException("Cannot move category under its own descendant")
            }

            val distance = node.right - node.left + 1
            val allCategories = repository.findAllOrderedByLeft()
            closeGapInTree(node, distance, allCategories)

            // Calculate new left and right positions
            val nodePositions: Pair<Int, Int> = getNodeGap(allCategories, newParent)
            val newLeft = nodePositions.component1()
            val newRight = nodePositions.component2()

            // Update category with new parent and position
            node.parent = newParent
            node.left = newLeft
            node.right = newRight
        }

        // Save and return updated category
        repository.save(node)
    }

    /**
     * Deletes a node from the nested set tree.
     *
     * @param node T The node to be deleted.
     */
    @Transactional
    protected open fun deleteNode(node: T) {
        val width = node.right - node.left + 1

        // Delete the subtree
        val subtree = repository.findSubtree(node.left, node.right)
        repository.deleteAll(subtree)
        closeGapInTree(node, width, repository.findAllOrderedByLeft())
    }

    /**
     * Closes the gap in the tree after a node is deleted.
     *
     * @param entity T The node that was deleted.
     * @param width Int The width of the gap to be closed.
     * @param allNodes List The list of all nodes in the tree.
     */
    protected fun closeGapInTree(entity: T, width: Int, allNodes: List<T>) {
        val updatedNodes = allNodes.filter { it.left > entity.right }
            .onEach {
                it.left = it.left - width
                it.right = it.right - width
            }
            .toMutableList()

        updatedNodes.addAll(allNodes.filter { it.right > entity.right && it.left < entity.right }
            .onEach { node -> node.right = node.right - width })
    }

    /**
     * Move a node in the tree.
     *
     * @param node      T The node to be moved.
     * @param direction MoveNodeDirection The direction in which the node will be moved (up or down).
     * @return T The updated node.
     */
    @Transactional
    protected open fun moveNode(node: T, direction: MoveNodeDirection): T = run {
        val parentId = node.parent?.id
        val sibling = if (direction == MoveNodeDirection.UP) {
            repository.findPrevSibling(parentId, node.left)
        } else {
            repository.findNextSibling(parentId, node.right)
        }

        if (sibling == null) return node

        val nodeWidth = node.right - node.left + 1
        val siblingWidth = sibling.right - sibling.left + 1

        val nodeSubtree = repository.findSubtree(node.left, node.right)
        val siblingSubtree = repository.findSubtree(sibling.left, sibling.right)

        nodeSubtree.forEach {
            it.left = it.left + TEMP_OFFSET
            it.right = it.right + TEMP_OFFSET
        }

        siblingSubtree.forEach {
            if (direction == MoveNodeDirection.UP) {
                it.left = it.left + nodeWidth
                it.right = it.right + nodeWidth
            } else {
                it.left = it.left - nodeWidth
                it.right = it.right - nodeWidth
            }
        }

        nodeSubtree.forEach {
            if (direction == MoveNodeDirection.UP) {
                it.left = it.left - TEMP_OFFSET - siblingWidth
                it.right = it.right - TEMP_OFFSET - siblingWidth
            } else {
                it.left = it.left - TEMP_OFFSET + siblingWidth
                it.right = it.right - TEMP_OFFSET + siblingWidth
            }
        }

        val all = mutableListOf<T>().apply {
            addAll(nodeSubtree)
            addAll(siblingSubtree)
        }
        saveAllNodes(all)

        node
    }

    /**
     * Check if a node is a descendant of another node.
     *
     * @param ancestor T The potential ancestor node.
     * @param descendant T The potential descendant node.
     * @return True if the descendant is a child of the ancestor, false otherwise.
     */
    protected fun isDescendant(ancestor: T, descendant: T): Boolean =
        descendant.left > ancestor.left && descendant.right < ancestor.right

    /**
     * Rebuild the tree structure.
     *
     * @param parent T? The parent node of the current node being processed.
     * @param allNodes List The list of all nodes in the tree.
     * @param currentLeft Int The current left value of the node being processed.
     * @return Int The right value of the node being processed.
     */
    @Transactional
    protected open fun rebuildTree(parent: T?, allNodes: List<T>, currentLeft: Int): Int = run {
        var left = currentLeft
        val parentId = parent?.id

        val children = allNodes.filter { node ->
            if (parentId == null) node.parent == null
            else node.parent?.id == parentId
        }.sortedBy { it.left }

        for (child in children) {
            val childLeft = left + 1
            val right = rebuildTree(child, allNodes, childLeft)
            child.left = childLeft
            child.right = right
            saveAllNodes(listOf(child))
            left = right
        }

        left + 1
    }

    /**
     * Rebuild the tree structure starting from the root node.
     *
     * @param parent   T The root node of the tree.
     * @param allNodes List The list of all nodes in the tree.
     * @return Int The right value of the root node.
     */
    @Transactional
    protected open fun rebuildTree(parent: T?, allNodes: List<T>): Int =
        rebuildTree(parent, allNodes, 0)

    /**
     * Save all nodes in the tree.
     *
     * @param nodes List The list of nodes to be saved.
     * @return List The list of saved nodes.
     */
    protected fun saveAllNodes(nodes: List<T>): List<T> = run {
        val savedNodes = repository.saveAll(nodes)
        repository.flush()
        savedNodes
    }
}
