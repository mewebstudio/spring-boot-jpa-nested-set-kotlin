package com.mewebstudio.springboot.jpa.nestedset.kotlin

/**
 * Enum representing the direction of a move operation in a nested set tree structure.
 * This enum is used to specify whether a node is being moved up or down in the tree.
 */
enum class MoveNodeDirection {
    /**
     * Direction to move the node up in the tree.
     */
    UP,

    /**
     * Direction to move the node down in the tree.
     */
    DOWN
}
