/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package arrange.model.subdivision;

import arrange.model.util.Position2D;
import arrange.model.util.Vector2D;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Label implements Position2D {

    private final String text;
    private final Vector2D position;

    public Label(String text, Vector2D position) {
        this.text = text;
        this.position = position;
    }

    public Label(Label other) {
        this.text = other.text;
        this.position = new Vector2D(other.position);
    }

    @Override
    public Vector2D getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }
}
