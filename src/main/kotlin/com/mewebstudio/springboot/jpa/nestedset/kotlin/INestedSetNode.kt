package com.mewebstudio.springboot.jpa.nestedset.kotlin

/**
 * Interface representing a node in a nested set tree structure.
 * This interface defines the basic properties and methods for a nested set node.
 *
 * @param ID The type of the identifier for the nested set node.
 * @param T The type of the nested set node itself.
 */
interface INestedSetNode<ID, T : INestedSetNode<ID, T>> {
    /**
     * The unique identifier of the node.
     */
    val id: ID

    /**
     * The left value of the node in the nested set.
     */
    var left: Int

    /**
     * The right value of the node in the nested set.
     */
    var right: Int

    /**
     * The level of the node in the nested set tree.
     */
    var parent: T?
}
