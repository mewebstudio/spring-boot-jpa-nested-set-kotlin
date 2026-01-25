package com.mewebstudio.springboot.jpa.nestedset.kotlin

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional

/**
 * Abstract service class for managing nested set trees.
 *
 * @param T The type of the nested set node.
 * @param ID The type of the identifier for the nested set node.
 */
abstract class AbstractNestedSetService<T : INestedSetNode<ID, T>, ID : Any>(
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
     * @param node T The new node to be created.
     * @param allNodes The list of all nodes in the tree. If null, fetches from the repository.
     * @return T The created node.
     */
    @Transactional
    protected open fun createNode(node: T, allNodes: List<T>? = null): T {
        val nodes = allNodes ?: repository.findAllOrderedByLeft()
        val gap: Pair<Int, Int> = getNodeGap(nodes, node.parent)

        return repository.save(node.apply {
            left = gap.first
            right = gap.second
        })
    }

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
        // Only process if the parent has changed
        if (hasParentChanged(node, newParent)) {
            // Check for cyclic reference
            if (newParent != null && isDescendant(node, newParent)) {
                throw IllegalArgumentException("Cannot move category under its own descendant")
            }

            moveNodeToNewParent(node, newParent)
        }

        // Save and return an updated category
        repository.save(node)
    }

    /**
     * Check if the parent of a node has changed.
     *
     * @param node T The node to check.
     * @param newParent T? The new parent node.
     * @return Boolean True if the parent has changed, false otherwise.
     */
    protected fun hasParentChanged(node: T, newParent: T?): Boolean {
        val currentParentId = node.parent?.id
        val newParentId = newParent?.id

        return when {
            currentParentId == null && newParentId == null -> false
            currentParentId == null || newParentId == null -> true
            else -> currentParentId != newParentId
        }
    }

    /**
     * Move a node and its subtree to a new parent.
     *
     * @param node T The node to be moved.
     * @param newParent T? The new parent node.
     */
    @Transactional
    protected open fun moveNodeToNewParent(node: T, newParent: T?) {
        val oldLeft = node.left
        val oldRight = node.right
        val subtreeWidth = oldRight - oldLeft + 1

        // Get all nodes in the subtree (including the node itself)
        val subtreeNodes = repository.findSubtree(oldLeft, oldRight)

        // Step 1: Temporarily move subtree out of the way using a large offset
        moveSubtreeToTempOffset(subtreeNodes)

        // Step 2: Close the gap left by the moved subtree
        closeGapInTree(oldRight, subtreeWidth)

        // Step 3: Calculate a new position for the subtree
        val newLeft = calculateNewPosition(newParent, subtreeWidth)
        val shift = newLeft - oldLeft

        // Step 4: Move subtree to the new position
        moveSubtreeFromTempToFinalPosition(oldLeft, oldRight, shift)

        // Update the node's parent reference
        node.parent = newParent
        // Refresh node's left and right from the saved values
        node.left = oldLeft + shift
        node.right = oldRight + shift
    }

    /**
     * Calculate the new position for inserting a subtree under a parent.
     * This method makes space for the subtree by shifting existing nodes.
     *
     * @param parent T? The parent node under which the subtree will be inserted.
     * @param subtreeWidth The width of the subtree being moved.
     * @return The new left position for the subtree.
     */
    @Transactional
    protected open fun calculateNewPosition(parent: T?, subtreeWidth: Int): Int {
        val allNodes = repository.findAllOrderedByLeft()
            .filter { it.left < TEMP_OFFSET } // Exclude temp nodes

        return if (parent == null) {
            // Insert at the end as a root node
            val maxRight = allNodes.maxOfOrNull { it.right } ?: 0
            maxRight + 1
        } else {
            // Re-fetch the parent to get updated values after gap closing
            val parentNode = repository.lockNode(parent.id)
                ?: throw EntityNotFoundException("Parent not found: ${parent.id}")

            val insertAt = parentNode.right

            // Shift nodes to make room for the subtree
            val nodesToShift = repository.findNodesToShift(insertAt - 1)
                .filter { it.left < TEMP_OFFSET } // Exclude temp nodes
                .filter { it.id != parentNode.id } // Exclude the parent itself

            nodesToShift.forEach { n ->
                if (n.left >= insertAt) n.left += subtreeWidth
                if (n.right >= insertAt) n.right += subtreeWidth
            }

            parentNode.right += subtreeWidth
            saveAllNodes(listOf(parentNode) + nodesToShift)

            insertAt
        }
    }

    /**
     * Deletes a node from the nested set tree.
     *
     * @param node T The node to be deleted.
     */
    @Transactional
    protected open fun deleteNode(node: T) {
        val width = node.right - node.left + 1
        val nodeRight = node.right

        // Delete the subtree
        val subtree = repository.findSubtree(node.left, node.right)
        repository.deleteAll(subtree)
        repository.flush()

        // Close the gap in the tree
        closeGapInTree(nodeRight, width)
    }

    /**
     * Closes the gap in the tree after a node is deleted or moved.
     *
     * @param deletedRight Int The right value of the deleted/moved node.
     * @param width Int The width of the gap to be closed.
     */
    @Transactional
    protected open fun closeGapInTree(deletedRight: Int, width: Int) {
        val allNodes = repository.findAllOrderedByLeft()
            .filter { it.left < TEMP_OFFSET } // Exclude temp nodes
        val nodesToUpdate = mutableListOf<T>()

        // Shift nodes that were to the right of the deleted subtree
        allNodes.forEach { node ->
            var updated = false
            if (node.left > deletedRight) {
                node.left -= width
                updated = true
            }
            if (node.right > deletedRight) {
                node.right -= width
                updated = true
            }
            if (updated) {
                nodesToUpdate.add(node)
            }
        }

        if (nodesToUpdate.isNotEmpty()) {
            saveAllNodes(nodesToUpdate)
        }
    }

    /**
     * Move a node in the tree.
     *
     * @param node      T The node to be moved.
     * @param direction MoveNodeDirection The direction in which the node will be moved (up or down).
     * @return T The updated node.
     */
    @Transactional
    protected open fun moveNode(node: T, direction: MoveNodeDirection): T {
        val parentId = node.parent?.id
        val sibling = if (direction == MoveNodeDirection.UP) {
            repository.findPrevSibling(parentId, node.left)
        } else {
            repository.findNextSibling(parentId, node.right)
        }

        if (sibling == null) return node

        val oldNodeLeft = node.left
        val oldNodeRight = node.right
        val nodeWidth = oldNodeRight - oldNodeLeft + 1
        val siblingWidth = sibling.right - sibling.left + 1

        // Calculate the shift for node and sibling
        val nodeShift = if (direction == MoveNodeDirection.UP) -siblingWidth else siblingWidth
        val siblingShift = if (direction == MoveNodeDirection.UP) nodeWidth else -nodeWidth

        val nodeSubtree = repository.findSubtree(oldNodeLeft, oldNodeRight)
        val siblingSubtree = repository.findSubtree(sibling.left, sibling.right)

        // Step 1: Move the node subtree to temp offset
        moveSubtreeToTempOffset(nodeSubtree)

        // Step 2: Move sibling subtree
        siblingSubtree.forEach {
            it.left += siblingShift
            it.right += siblingShift
        }
        saveAllNodes(siblingSubtree)

        // Step 3: Move the node subtree from temp to the final position
        moveSubtreeFromTempToFinalPosition(oldNodeLeft, oldNodeRight, nodeShift)

        // Update the original node reference with new values
        node.left = oldNodeLeft + nodeShift
        node.right = oldNodeRight + nodeShift

        return node
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
     * @param nodesToSave MutableList The list of nodes to be saved after rebuilding.
     * @return Int The right value of the node being processed.
     */
    protected open fun rebuildTreeRecursive(
        parent: T?,
        allNodes: List<T>,
        currentLeft: Int,
        nodesToSave: MutableList<T>
    ): Int {
        var left = currentLeft
        val parentId = parent?.id

        val children = allNodes.filter { node ->
            if (parentId == null) node.parent == null
            else node.parent?.id == parentId
        }.sortedBy { it.left }

        for (child in children) {
            val childLeft = left + 1
            val right = rebuildTreeRecursive(child, allNodes, childLeft, nodesToSave)
            child.left = childLeft
            child.right = right
            nodesToSave.add(child)
            left = right
        }

        return left + 1
    }

    /**
     * Rebuild the tree structure starting from the root node.
     *
     * @param parent   T The root node of the tree.
     * @param allNodes List The list of all nodes in the tree.
     * @return Int The right value of the root node.
     */
    @Transactional
    protected open fun rebuildTree(parent: T?, allNodes: List<T>): Int {
        val nodesToSave = mutableListOf<T>()
        val result = rebuildTreeRecursive(parent, allNodes, 0, nodesToSave)
        if (nodesToSave.isNotEmpty()) {
            saveAllNodes(nodesToSave)
        }
        return result
    }

    /**
     * Save all nodes in the tree.
     *
     * @param nodes List The list of nodes to be saved.
     */
    protected fun saveAllNodes(nodes: List<T>) {
        repository.saveAll(nodes)
        repository.flush()
    }

    /**
     * Move a subtree to a temporary offset position.
     *
     * @param subtreeNodes List The list of nodes in the subtree.
     */
    protected fun moveSubtreeToTempOffset(subtreeNodes: List<T>) {
        subtreeNodes.forEach {
            it.left += TEMP_OFFSET
            it.right += TEMP_OFFSET
        }
        saveAllNodes(subtreeNodes)
    }

    /**
     * Move a subtree from the temporary offset to the final position.
     *
     * @param oldLeft Int The original left value of the subtree.
     * @param oldRight Int The original right value of the subtree.
     * @param shift Int The shift to apply to the subtree.
     */
    protected fun moveSubtreeFromTempToFinalPosition(oldLeft: Int, oldRight: Int, shift: Int) {
        val movedSubtreeNodes = repository.findSubtree(oldLeft + TEMP_OFFSET, oldRight + TEMP_OFFSET)
        movedSubtreeNodes.forEach {
            it.left = it.left - TEMP_OFFSET + shift
            it.right = it.right - TEMP_OFFSET + shift
        }
        saveAllNodes(movedSubtreeNodes)
    }
}
