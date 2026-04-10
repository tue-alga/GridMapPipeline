package arrange.ipe.style;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public class Layout {

    private Integer paperSizeX = null;
    private Integer paperSizeY = null;
    private Integer originX = null;
    private Integer originY = null;
    private Integer frameSizeX = null;
    private Integer frameSizeY = null;
    private Boolean crop = null;

    public Layout() {
    }

    public Integer getPaperSizeX() {
        return paperSizeX;
    }

    public Integer getPaperSizeY() {
        return paperSizeY;
    }

    public void setPaperSize(int x, int y) {
        paperSizeX = x;
        paperSizeY = y;
    }

    public Integer getOriginX() {
        return originX;
    }

    public Integer getOriginY() {
        return originY;
    }

    public void setOrigin(int x, int y) {
        originX = x;
        originY = y;
    }

    public Integer getFrameSizeX() {
        return frameSizeX;
    }

    public Integer getFrameSizeY() {
        return frameSizeY;
    }

    public void setFrameSize(int x, int y) {
        frameSizeX = x;
        frameSizeY = y;
    }

    public Boolean getCrop() {
        return crop;
    }

    public void setCrop(Boolean crop) {
        this.crop = crop;
    }

    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<layout paper=\"");
        sb.append(paperSizeX);
        sb.append(" ");
        sb.append(paperSizeY);
        sb.append("\" origin=\"");
        sb.append(originX);
        sb.append(" ");
        sb.append(originY);
        sb.append("\" frame=\"");
        sb.append(frameSizeX);
        sb.append(" ");
        sb.append(frameSizeY);
        sb.append("\"");
        if (crop != null) {
            sb.append(" crop=\"");
            if (crop == true) {
                sb.append("yes\"");
            } else {
                sb.append("no\"");
            }
        }
        sb.append("/>");
        sb.append(System.lineSeparator());
        return sb.toString();
    }
}
