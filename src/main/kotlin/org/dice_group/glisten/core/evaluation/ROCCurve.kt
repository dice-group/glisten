package org.dice_group.glisten.core.evaluation

private enum class DIRECTION{
    UP, RIGHT, DIAGONALLY
}

/**
 * Creates the underlying ROC Curve.
 *
 * Shamelessy stolen from gerbil KBC
 */
class ROCCurve(val trueStmts: Int, val falseStmts: Int) {

    private val points = mutableListOf<Point>()

    private val stepLengthUp: Double = 1.0/trueStmts
    private val stepLengthRight: Double = 1.0/falseStmts

    private var upStepsCount = 0.0
    private var rightStepsCount = 0.0
    private var lastDir: DIRECTION? = null

    override fun toString(): String {
        return points.toString()
    }

    fun addPoint(point: Point){
        this.points.add(point)
    }

    fun addPoint(x: Double, y: Double){
        val newPoint = Point(0.0, 0.0)
        newPoint.setLocation(x, y)
        points.add(newPoint)
    }

    /**
     * Adds a new point to the curve by going one step up.
     */
    fun addUp() {
        ++upStepsCount
        var newY = 0.0
        newY = if (upStepsCount >= trueStmts) {
            // We want to end up at 1.0 so simply add it instead of trying to
            // calculate it
            // and end up with 0.999...
            1.0
        } else {
            upStepsCount * stepLengthUp
        }
        addUp(newY)
    }

    /**
     * Adds a new point in the upper direction with the given Y value and the X
     * value of the last point. If the last point has already been set using a
     * step up, this point is reused instead of creating a new point.
     *
     * @param newY
     * the y value of the new point
     */
    private fun addUp(newY: Double) {
        var lastX = 0.0
        if (!points.isEmpty()) {
            val last = points[points.size - 1]
            lastX = last.x
            if (DIRECTION.UP == lastDir) {
                last.setLocation(lastX, newY)
                return
            }
        }
        val newP = Point()
        newP.setLocation(lastX, newY)
        points.add(newP)
        lastDir = DIRECTION.UP
    }

    /**
     * Adds a new point to the curve by going one step right.
     */
    fun addRight() {
        val newX: Double
        ++rightStepsCount
        newX = if (rightStepsCount >= falseStmts) {
            // We want to end up at 1.0 so simply add it instead of trying to
            // calculate it
            // and end up with 0.999...
            1.0
        } else {
            rightStepsCount * stepLengthRight
        }
        addRight(newX)
    }

    /**
     * Adds a new point in the right direction with the given X value and the Y
     * value of the last point. If the last point has already been set using a
     * step to the right, this point is reused instead of creating a new point.
     *
     * @param newX
     * the x value of the new point
     */
    private fun addRight(newX: Double) {
        var lastY = 0.0
        if (!points.isEmpty()) {
            val last = points[points.size - 1]
            lastY = last.y
            if (DIRECTION.RIGHT == lastDir) {
                last.setLocation(newX, lastY)
                return
            }
        }
        val newP = Point(newX, lastY)
        points.add(newP)
        lastDir = DIRECTION.RIGHT
    }


    fun addDiagonally(stepsUp: Int, stepsRight: Int) {
        // If we start with a triangle we have to add (0,0)
        val last: Point
        if (points.isEmpty()) {
            last = Point(0.0, 0.0)
            points.add(last)
        } else {
            last = points[points.size - 1]
        }
        var x = 0.0
        rightStepsCount += stepsRight
        x = if (rightStepsCount >= falseStmts) {
            // We want to end up at 1.0 so simply add it instead of trying to
            // calculate it and end up with 0.999...
            1.0
        } else {
            last.x + stepsRight * stepLengthRight
        }
        var y = 0.0
        upStepsCount += stepsUp
        y = if (upStepsCount >= trueStmts) {
            // We want to end up at 1.0 so simply add it instead of trying to
            // calculate it and end up with 0.999...
            1.0
        } else {
            last.y + stepsUp * stepLengthUp
        }
        points.add(Point(x, y))
        lastDir = DIRECTION.DIAGONALLY
    }

    fun calculateAUC(): Double {
        var auc = 0.0
        var aup: Double
        var pointA: Point
        var pointB = points[0]
        for (i in 1 until points.size) {
            pointA = pointB
            pointB = points[i]
            // calculate area under the points (rectangle)
            if (pointB.x != pointA.x) {
                // if the two points are a step to the right
                if (pointB.y == pointA.y) {
                    aup = pointA.y * (pointB.x - pointA.x)
                } else {
                    // this is a diagonal
                    // rectangle "under B"
                    aup = pointA.y * (pointB.x - pointA.x)
                    // triangle from A to B
                    aup += 0.5 * (pointB.y - pointA.y) * (pointB.x - pointA.x)
                }
                auc += aup
            }
        }
        return auc
    }
}