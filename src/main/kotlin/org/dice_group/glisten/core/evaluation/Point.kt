package org.dice_group.glisten.core.evaluation

class Point(var x: Double =0.0, var y: Double =0.0) {


    fun setLocation(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[ x : ").append(x).append(", y : ").append(y).append(" ]")
        return builder.toString()
    }

    override fun equals(other: Any?): Boolean {
        if(other is Point){
            return other.x == other.x &&
                    other.y == other.y
        }
        return false
    }
}