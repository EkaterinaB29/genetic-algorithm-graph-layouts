import java.io.Serializable;

class Node implements Serializable {

    private final int id;
    public double x;
    public double y;

    public Node(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public Node(Node original) {
        this.id = original.id;
        this.x = original.x;
        this.y = original.y;
    }

    public int getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public static double euclideanDistance(Node one, Node two) {
        double deltaX = one.getX() - two.getX();
        double deltaY = two.getY() - one.getY();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }


}