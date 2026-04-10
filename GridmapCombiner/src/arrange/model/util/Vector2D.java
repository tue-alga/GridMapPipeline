package arrange.model.util;

/**
 * Class to model a 2D vector (physics/mathematics vector). As a rule of thumb:
 * static functions never modify the arguments. They always return a reference
 * to a new object. Instance methods might work as compound assignment operators
 * (+=, -=, *=, etc). See comments below for more details on each particular
 * method.
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Vector2D {

    private double x;
    private double y;

    /**
     * Creates a new vector with x and y coordinates.
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a new vector with the same coordinates as v.
     */
    public Vector2D(Vector2D v) {
        this.x = v.x;
        this.y = v.y;
    }

    /**
     * Returns the x-coordinate of this vector.
     */
    public double getX() {
        return x;
    }

    /**
     * Sets the x-coordinate of this vector.
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Returns the y-coordinate of this vector.
     */
    public double getY() {
        return y;
    }

    /**
     * Sets the y-coordinate of this vector.
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Adds v to this vector and assigns the result to this vector. Returns a
     * reference to this vector.
     */
    public Vector2D add(Vector2D v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    /**
     * Subtracts v from this vector and assigns the result to this vector.
     * Returns a reference to this vector.
     */
    public Vector2D subtract(Vector2D v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    /**
     * Multiplies this vector by a scalar constant c and assigns the result to
     * this vector. Returns a reference to this vector.
     */
    public Vector2D multiply(double c) {
        this.x *= c;
        this.y *= c;
        return this;
    }

    /**
     * Transforms this vector in such a way that it maintains the same direction
     * but has norm 1. Returns a reference to this vector.
     */
    public Vector2D normalize() {
        double norm = this.norm();
        if (norm != 0.0) {
            x /= norm;
            y /= norm;
        }
        return this;
    }

    /**
     * Returns the dot product of this vector and v.
     */
    public double dotProduct(Vector2D v) {
        return this.x * v.x + this.y * v.y;
    }

    /**
     * Returns the norm of this vector.
     */
    public double norm() {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * Returns the quadrant of this vector.
     */
    public Quadrant quadrant() {
        if (x > 0) {
            if (y > 0) {
                return Quadrant.FIRST;
            } else if (y < 0) {
                return Quadrant.FOURTH;
            } else {
                return Quadrant.POSITIVE_X_AXIS;
            }
        } else if (x < 0) {
            if (y > 0) {
                return Quadrant.SECOND;
            } else if (y < 0) {
                return Quadrant.THIRD;
            } else {
                return Quadrant.NEGATIVE_X_AXIS;
            }
        } else {
            if (y > 0) {
                return Quadrant.POSITIVE_Y_AXIS;
            } else if (y < 0) {
                return Quadrant.NEGATIVE_Y_AXIS;
            } else {
                return Quadrant.ORIGIN;
            }
        }

    }

    /**
     * Returns a new vector corresponding to u + v.
     */
    public static Vector2D sum(Vector2D u, Vector2D v) {
        return new Vector2D(u.x + v.x, u.y + v.y);
    }

    /**
     * Returns a new vector corresponding to u - v.
     */
    public static Vector2D difference(Vector2D u, Vector2D v) {
        return new Vector2D(u.x - v.x, u.y - v.y);
    }

    /**
     * Returns a new vector corresponding to c*v.
     */
    public static Vector2D product(Vector2D v, double c) {
        return new Vector2D(v.x * c, v.y * c);
    }

    /**
     * Returns a new vector corresponding to c*v.
     */
    public static Vector2D product(double c, Vector2D v) {
        return new Vector2D(v.x * c, v.y * c);
    }

    /**
     * Returns a new normalized vector (norm 1).
     */
    public static Vector2D normalize(Vector2D v) {
        return new Vector2D(v).normalize();
    }

    /**
     * Returns the dot product of u and v.
     */
    public static double dotProduct(Vector2D u, Vector2D v) {
        return u.dotProduct(v);
    }

    /**
     * Returns the norm of v.
     */
    public static double norm(Vector2D v) {
        return v.norm();
    }

    /**
     * Returns the quadrant of this vector.
     */
    public static Quadrant quadrant(Vector2D v) {
        return v.quadrant();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Vector2D)) {
            return false;
        }
        Vector2D v = (Vector2D) obj;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(v.x)) {
            return false;
        }
        if (Double.doubleToLongBits(y) != Double.doubleToLongBits(v.y)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) Double.doubleToLongBits(x);
        result = prime * result + (int) Double.doubleToLongBits(y);
        return result;
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }

    public static enum Quadrant {

        ORIGIN,
        POSITIVE_X_AXIS,
        FIRST,
        POSITIVE_Y_AXIS,
        SECOND,
        NEGATIVE_X_AXIS,
        THIRD,
        NEGATIVE_Y_AXIS, FOURTH
    }
}
