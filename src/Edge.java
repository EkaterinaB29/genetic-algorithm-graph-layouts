import java.io.Serializable;
import java.util.ArrayList;

import static java.awt.geom.Line2D.linesIntersect;

public final class Edge implements Serializable {
    int origin;
    int destination;

    public Edge(int origin, int destination) {
        this.origin = origin;
        this.destination = destination;
    }

    public boolean intersects(Edge other, ArrayList<Node> nodes) {
        return linesIntersect(nodes.get(other.origin).getX(), nodes.get(other.destination).getY(), nodes.get(this.origin).getX(), nodes.get(this.destination).getY(), other.getOrigin(nodes).getX(), other.getOrigin(nodes).getY(), other.getDestination(nodes).getX(), other.getDestination(nodes).getY());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Edge other = (Edge) obj;
        return (this.origin != this.destination && other.origin != other.destination)
                && ((this.origin == other.origin && this.destination == other.destination) ||
                (this.origin == other.destination && this.destination == other.origin));
    }

    @Override
    public int hashCode() {
        int prime = 29;
        int result = 1;
        result = prime * result + Math.min(origin, destination);
        result = prime * result + Math.max(origin, destination);
        return result;
    }


    public Node getOrigin(ArrayList<Node> nodes) {
        return nodes.get(origin);
    }

    public Node getDestination(ArrayList<Node> nodes) {
        return nodes.get(destination);
    }
}
