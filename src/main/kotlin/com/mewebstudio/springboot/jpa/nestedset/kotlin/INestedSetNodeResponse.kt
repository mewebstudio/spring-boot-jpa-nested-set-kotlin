package com.mewebstudio.springboot.jpa.nestedset.kotlin

/**
 * Interface representing a response node in a nested set tree structure.
 * This interface extends the basic properties of a nested set node with additional methods for handling children.
 *
 * @param <ID> Type of the node identifier.
 */
interface INestedSetNodeResponse<ID> {
    /**
     * The unique identifier of the node.
     */
    val id: ID

    /**
     * The left value of the node in the nested set.
     */
    val left: Int

    /**
     * The right value of the node in the nested set.
     */
    val right: Int

    /**
     * The level of the node in the nested set tree.
     */
    val children: List<INestedSetNodeResponse<ID>>?

    /**
     * Set the list of child nodes for this node.
     *
     * @param children List The list of child nodes to set.
     * @return INestedSetNodeResponse The current node with updated children.
     */
    fun withChildren(children: List<INestedSetNodeResponse<ID>>): INestedSetNodeResponse<ID>
}
