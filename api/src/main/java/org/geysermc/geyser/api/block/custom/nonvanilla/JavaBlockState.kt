package org.geysermc.geyser.api.block.custom.nonvanilla

import org.checkerframework.checker.index.qual.NonNegative
import org.geysermc.geyser.api.GeyserApi

interface JavaBlockState {
    /**
     * Gets the identifier of the block state
     * 
     * @return the identifier of the block state
     */
    fun identifier(): String

    /**
     * Gets the Java ID of the block state
     * 
     * @return the Java ID of the block state
     */
    fun javaId(): @NonNegative Int

    /**
     * Gets the state group ID of the block state
     * 
     * @return the state group ID of the block state
     */
    fun stateGroupId(): @NonNegative Int

    /**
     * Gets the block hardness of the block state
     * 
     * @return the block hardness of the block state
     */
    fun blockHardness(): @NonNegative Float

    /**
     * Gets whether the block state is waterlogged
     * 
     * @return whether the block state is waterlogged
     */
    fun waterlogged(): Boolean

    /**
     * Gets the collision of the block state
     * 
     * @return the collision of the block state
     */
    fun collision(): Array<JavaBoundingBox?>?

    /**
     * Gets whether the block state can be broken with hand
     * 
     * @return whether the block state can be broken with hand
     */
    fun canBreakWithHand(): Boolean

    /**
     * Gets the pick item of the block state
     * 
     * @return the pick item of the block state
     */
    @Deprecated("the pick item is sent by the Java server")
    fun pickItem(): String?

    /**
     * Gets the piston behavior of the block state
     * 
     * @return the piston behavior of the block state
     */
    fun pistonBehavior(): String?

    /**
     * Gets whether the block state has a block entity
     * 
     * @return whether the block state has block entity
     */
    @Deprecated(
        """Does not have an effect. If you were using this to
      set piston behavior, use {@link #pistonBehavior()} instead."""
    )
    fun hasBlockEntity(): Boolean

    interface Builder {
        fun identifier(identifier: String): Builder?

        fun javaId(javaId: @NonNegative Int): Builder?

        fun stateGroupId(stateGroupId: @NonNegative Int): Builder?

        fun blockHardness(blockHardness: @NonNegative Float): Builder?

        fun waterlogged(waterlogged: Boolean): Builder?

        fun collision(collision: Array<JavaBoundingBox>?): Builder?

        fun canBreakWithHand(canBreakWithHand: Boolean): Builder?

        @Deprecated("")
        fun pickItem(pickItem: String?): Builder?

        fun pistonBehavior(pistonBehavior: String?): Builder?

        @Deprecated(
            """Does not have an effect. If you were using this to
               * set piston behavior, use {@link #pistonBehavior(String)} instead."""
        )
        fun hasBlockEntity(hasBlockEntity: Boolean): Builder?

        fun build(): JavaBlockState?
    }

    companion object {
        /**
         * Creates a new [JavaBlockState.Builder] instance
         * 
         * @return a new [JavaBlockState.Builder] instance
         */
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
