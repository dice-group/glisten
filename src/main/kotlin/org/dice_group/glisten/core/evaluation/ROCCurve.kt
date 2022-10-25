package org.dice_group.glisten.core.evaluation

/**
 * Enum providing the possible Directions for a [ROCCurve]
 */
enum class DIRECTION{
    UP, RIGHT
}

/**
 * Creates the underlying ROC Curve.
 *
 * Shamelessly stolen from gerbil KBC
 */
class ROCCurve(private val trueStmts: Int, private val falseStmts: Int) {

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
        val newPoint = Point(x, y)
        points.add(newPoint)
    }


    /**
     * Adds a new point to the curve by going one step up.
     */
    fun addUp() {
        ++upStepsCount
        val newY = if (upStepsCount >= trueStmts) {
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
        if (points.isNotEmpty()) {
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
        ++rightStepsCount
        val newX = if (rightStepsCount >= falseStmts) {
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
        if (points.isNotEmpty()) {
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


    /**
     * finalize the curve by adding the last point to 1.0/1.0 if it's not existing
     * this allows calculating AUC scores for ROC curves build soley on true statements.
     */
    private fun finalize(){
        val lastPoint = points.last()
        if(lastPoint.x != 1.0 || lastPoint.y != 1.0){
            points.add(Point(1.0, 1.0))
        }
    }

    /**
     * Finalizes (adding a point to (1.0, 1.0)) and then calculating the area under the curve \[AUC\] from the ROCCurve
     *
     * @return the AUC score for this ROC curve
     */
    fun calculateAUC(): Double {
        finalize()
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
        if(auc.isNaN()){
            println("\n[!!] NaN found: $points")
        }
        return auc
    }

    override fun equals(other: Any?): Boolean {
        if (other is ROCCurve){
            return other.points == points &&
                    other.trueStmts == trueStmts &&
                    other.falseStmts == falseStmts
        }
        return false
    }
}