/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package assign.alignments;

import assign.Alignment;
import common.Partition;
import common.util.Transform;

/**
 *
 * @author wmeulema
 */
public class GuideAlignment extends Alignment {

    public GuideAlignment() {
        super("GuideAlign");
    }

    @Override
    public Transform construct(Partition p) {
        return p.guide.mapToGridTransform();
    }
    
}
