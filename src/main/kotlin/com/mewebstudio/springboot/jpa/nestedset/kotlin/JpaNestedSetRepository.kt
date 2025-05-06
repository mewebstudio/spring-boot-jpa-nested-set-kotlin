package com.mewebstudio.springboot.jpa.nestedset.kotlin

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param

/**
 * Repository interface for managing nested set nodes.
 * This interface extends JpaRepository and provides additional methods for nested set operations.
 *
 * @param T Type of the nested set node.
 * @param ID Type of the identifier for the nested set node.
 */
@NoRepositoryBean
interface JpaNestedSetRepository<T : INestedSetNode<ID, T>, ID> : JpaRepository<T, ID> {
    /**
     * Find all nodes in the tree, ordered by their left value.
     * This method retrieves all nodes in the tree structure.
     *
     * @return A list of all nodes ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e ORDER BY e.left")
    fun findAllOrderedByLeft(): List<T>

    /**
     * Find all root nodes in the tree.
     * This method retrieves nodes where the parent is null.
     *
     * @return A list of all root nodes ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.parent IS NULL ORDER BY e.left")
    fun findRootNodes(): List<T>

    /**
     * Find all leaf nodes in the tree.
     * Retrieves nodes where right = left + 1, meaning they have no children and are leaf nodes.
     *
     * @return A list of all leaf nodes.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left + 1 = e.right")
    fun findLeafNodes(): List<T>

    /**
     * Find previous sibling of a node by its parentId and left value.
     *
     * @param parentId The ID of the parent node. If null, it searches for root nodes.
     * @param left The left value of the node to find the previous sibling for.
     * @return An optional containing the previous sibling node if found, otherwise empty.
     */
    @Query(
        """
        SELECT e FROM #{#entityName} e
        WHERE e.right < :left
          AND (
            (:parentId IS NULL AND e.parent IS NULL)
            OR (e.parent.id = :parentId)
          )
        ORDER BY e.left DESC LIMIT 1
        """
    )
    fun findPrevSibling(@Param("parentId") parentId: ID?, @Param("left") left: Int): T?

    /**
     * Find next sibling of a node by its parentId and left value.
     *
     * @param parentId The ID of the parent node. If null, it searches for root nodes.
     * @param right The right value of the node to find the next sibling for.
     * @return An optional containing the next sibling node if found, otherwise empty.
     */
    @Query(
        """
        SELECT e FROM #{#entityName} e
        WHERE e.left > :right
          AND (
            (:parentId IS NULL AND e.parent IS NULL)
            OR (e.parent.id = :parentId)
          )
        ORDER BY e.left ASC LIMIT 1
        """
    )
    fun findNextSibling(@Param("parentId") parentId: ID?, @Param("right") right: Int): T?

    /**
     * Find all children of a given parent node.
     * This method retrieves nodes where the parent ID matches the provided parentId.
     *
     * @param parentId The ID of the parent node. If null, it searches for root nodes.
     * @return A list of all child nodes ordered by their left value.
     */
    @Query(
        """
        SELECT e FROM #{#entityName} e
        WHERE (:parentId IS NULL AND e.parent IS NULL)
           OR (:parentId IS NOT NULL AND e.parent.id = :parentId)
        ORDER BY e.left
        """
    )
    fun findChildren(@Param("parentId") parentId: ID?): List<T>

    /**
     * Find all siblings of a given node.
     * This method retrieves nodes where the parent ID matches the provided parentId
     * and the node ID is not equal to the provided selfId.
     * This is used to find nodes that share the same parent.
     *
     * @param parentId The ID of the parent node.
     * @param selfId The ID of the node itself. This is used to exclude the node from the results.
     * @return A list of all sibling nodes ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.parent.id = :parentId AND e.id <> :selfId ORDER BY e.left")
    fun findSiblings(@Param("parentId") parentId: ID, @Param("selfId") selfId: ID): List<T>

    /**
     * Find all ancestors of a given node.
     * This method retrieves nodes where the left value is less than the provided left value
     * and the right value is greater than the provided right value.
     *
     * @param left The left value of the node.
     * @param right The right value of the node.
     * @return A list of all ancestor nodes ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left < :left AND e.right > :right ORDER BY e.left DESC")
    fun findAncestors(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Find all descendants of a given node.
     * This method retrieves nodes where the left value is greater than the provided left value
     * and the right value is less than the provided right value.
     *
     * @param left The left value of the node.
     * @param right The right value of the node.
     * @return A list of all descendant nodes ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left > :left AND e.right < :right ORDER BY e.left")
    fun findDescendants(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Find all subtrees of a regular node, including its subtrees.
     * This method retrieves nodes where the left value is greater than or equal to the provided left value
     * and the right value is less than or equal to the provided right value.
     *
     * @param left The left value of the node.
     * @param right The right value of the node.
     * @return A list of all subtree nodes ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left >= :left AND e.right <= :right ORDER BY e.left ASC")
    fun findSubtree(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Find all the nodes (top level nodes) that cover the given range.
     * This method retrieves nodes where the left value is less than or equal to the provided left value
     * and the right value is greater than or equal to the provided right value.
     *
     * @param left The left value of the range.
     * @param right The right value of the range.
     * @return A list of all nodes that cover the specified range ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left <= :left AND e.right >= :right ORDER BY e.left")
    fun findContaining(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Find all the node that exactly matches the given left and right values.
     * This method retrieves nodes where the left value is equal to the provided left value
     * and the right value is equal to the provided right value.
     *
     * @param left The left value of the node.
     * @param right The right value of the node.
     * @return A list of all nodes that exactly match the specified left and right values ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left = :left AND e.right = :right ORDER BY e.left")
    fun findExact(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Retrieves all ancestor nodes of a given node based on its left and right values, excluding the node itself.
     * This method retrieves nodes where the left value is less than the provided left value
     * and the right value is greater than the provided right value.
     *
     * @param left The left value of the node.
     * @param right The right value of the node.
     * @return A list of all ancestor nodes excluding the node itself ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left < :left AND e.right > :right ORDER BY e.left")
    fun findExactExcluding(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Retrieves nodes with a right value greater than the specified value.
     * Used when shifting nodes during insertion or deletion to maintain nested set integrity.
     * This method retrieves nodes where the right value is greater than the provided right value.
     * This is used to find nodes that need to be shifted when inserting a new node.
     *
     * @param right The right value to compare against.
     * @return A list of all nodes that need to be shifted ordered by their left value.
     */
    @Query("SELECT c FROM #{#entityName} c WHERE c.right > :right")
    fun findNodesToShift(@Param("right") right: Int): List<T>

    /**
     * Find a node by its left value.
     * This method retrieves a single node where the left value matches the provided left value.
     *
     * @param left The left value of the node to find.
     * @return An optional containing the node if found, otherwise empty.
     */
    fun findByLeft(left: Int): T?

    /**
     * Find a node by its right value.
     * This method retrieves a single node where the right value matches the provided right value.
     *
     * @param right The right value of the node to find.
     * @return An optional containing the node if found, otherwise empty.
     */
    fun findByRight(right: Int): T?

    /**
     * Find a node by its left and right values.
     * This method retrieves a single node where the left and right values match the provided values.
     *
     * @param left The left value of the node to find.
     * @param right The right value of the node to find.
     * @return An optional containing the node if found, otherwise empty.
     */
    fun findByLeftAndRight(left: Int, right: Int): T?

    /**
     * Find all nodes that are children of a given parent node.
     * This method retrieves nodes where the parent ID matches the provided parentId.
     *
     * @param parentId The ID of the parent node.
     * @return A list of all child nodes ordered by their left value.
     */
    @Query("SELECT c FROM #{#entityName} c WHERE c.parent.id = :parentId ORDER BY c.left")
    fun findByParentId(@Param("parentId") parentId: ID): List<T>

    /**
     * Find all nodes with left value between the specified range.
     * This is useful when moving nodes within the nested set.
     *
     * @param left The left value of the range.
     * @param right The right value of the range.
     * @return A list of all nodes with left value between the specified range ordered by their left value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left BETWEEN :left AND :right ORDER BY e.left")
    fun findByLeftBetween(@Param("left") left: Int, @Param("right") right: Int): List<T>

    /**
     * Lock a node by its ID.
     * Prevents race conditions by locking a node with a pessimistic write lock.
     * Especially useful for nested set manipulations.
     * This method retrieves a single node where the ID matches the provided ID and locks it for writing.
     *
     * @param id The ID of the node to lock.
     * @return An optional containing the locked node if found, otherwise empty.
     */
    @Query("SELECT c FROM #{#entityName} c WHERE c.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun lockNode(@Param("id") id: ID): T?
}
