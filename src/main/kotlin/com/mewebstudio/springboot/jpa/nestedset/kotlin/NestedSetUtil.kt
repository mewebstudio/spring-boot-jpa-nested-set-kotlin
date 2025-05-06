package com.mewebstudio.springboot.jpa.nestedset.kotlin


/**
 * Utility class for working with nested set trees.
 * This class provides methods to build a tree structure from a list of nested set nodes.
 */
object NestedSetUtil {
    /**
     * Build a tree structure from a list of nested set nodes.
     *
     * @param nodes List of nested set nodes.
     * @param convert Function to convert a node to its response type.
     * @param ID Type of the node identifier.
     * @param E Type of the nested set node.
     * @param T Type of the response node.
     * @return List of top-level nodes with their children.
     */
    fun <ID, E : INestedSetNode<ID, E>, T : INestedSetNodeResponse<ID>> tree(
        nodes: List<E>,
        convert: (E) -> T
    ): List<T> = run {
        if (nodes.isEmpty()) return emptyList()

        val responseMap = mutableMapOf<ID, T>()
        val nodeMap = nodes.associateBy { it.id }

        // Map nodes to their response objects
        nodes.forEach { responseMap[it.id] = convert(it) }

        // Group children by parent ID
        val childrenByParentId = mutableMapOf<ID, MutableList<ID>>()
        nodes.forEach {
            val parent = it.parent
            val parentId = parent?.id
            if (parentId != null && nodeMap.contains(parentId)) {
                childrenByParentId.computeIfAbsent(parentId) { mutableListOf() }.add(it.id)
            }
        }

        // Build tree by assigning children to parents
        nodes.sortedByDescending { it.left }.forEach { node ->
            val parent = responseMap[node.id]
            val children = childrenByParentId[node.id]
                ?.mapNotNull { responseMap[it] }
                .orEmpty()

            @Suppress("UNCHECKED_CAST")
            val withChildren = parent?.withChildren(children as List<INestedSetNodeResponse<ID>>) as T?
            if (withChildren != null) {
                responseMap[node.id] = withChildren
            }
        }

        // Find top-level nodes among the provided list
        nodes.filter {
            val parent = it.parent
            val parentId = parent?.id
            parentId == null || !nodeMap.contains(parentId)
        }.mapNotNull { responseMap[it.id] }
    }
}
