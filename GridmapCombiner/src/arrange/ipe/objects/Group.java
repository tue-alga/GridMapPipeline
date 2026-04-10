package arrange.ipe.objects;

import java.util.ArrayList;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class Group extends IpeObject {

    private ArrayList<IpeObject> objects = new ArrayList<>();

    public Group() {
    }

    public Iterable<IpeObject> objects() {
        return objects;
    }

    public IpeObject getObject(int i) {
        return objects.get(i);
    }

    public int getNumObjects() {
        return objects.size();
    }

    public void addObject(IpeObject object) {
        objects.add(object);
    }

    public IpeObject removeObject(int i) {
        return objects.remove(i);
    }

    @Override
    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<group");
        sb.append(commonAttributes());
        sb.append(">");
        sb.append(System.lineSeparator());
        for (IpeObject object : objects) {
            sb.append(object.toXMLString());
        }
        sb.append("</group>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
