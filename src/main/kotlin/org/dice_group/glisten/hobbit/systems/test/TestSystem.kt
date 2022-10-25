package org.dice_group.glisten.hobbit.systems.test

import org.dice_group.glisten.hobbit.systems.AbstractGlistenHobbitSystem
import java.io.File
import kotlin.random.Random

/**
 * Simple test system which just randomly sets scores from 0.0 to 1.0
 */
class TestSystem : AbstractGlistenHobbitSystem() {

    private val seed = 123;

    override fun generateRecommendationScores(source: File, targets: ArrayList<File>): List<Pair<File, Double>> {
        val rand = Random(seed)
        val ret = mutableListOf<Pair<File, Double>>()
        targets.forEach{
            ret.add(Pair(it, rand.nextDouble(1.0)))
        }
        return ret
    }


}