package org.dice_group.glisten.core.evaluation

class Point(x: Double =0.0, y: Double =0.0) {


    var x = 0.0
    var y = 0.0


    fun setLocation(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[ x : ").append(x).append(", y : ").append(y).append(" ]")
        return builder.toString()
    }
}