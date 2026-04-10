package arrange.ipe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import arrange.ipe.attributes.ColorAttribute;
import arrange.ipe.attributes.MatrixAttribute;
import arrange.ipe.attributes.PenAttribute;
import arrange.ipe.attributes.PointAttribute;
import arrange.ipe.elements.Layer;
import arrange.ipe.objects.Group;
import arrange.ipe.objects.IpeObject;
import arrange.ipe.objects.Path;
import arrange.ipe.objects.Text;
import arrange.ipe.style.Gradient;
import arrange.ipe.style.StyleSheet;
import arrange.ipe.style.SymbolicColor;
import arrange.ipe.style.SymbolicPen;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Rafael Cano <rgcano at gmail.com>
 */
public final class XMLParser {

    private Document document = new Document();
    private StyleSheet openStyleSheet = null;
    private Gradient openGradient = null;
    private Path openPath = null;
    private Text openText = null;
    private ArrayDeque<Group> openGroups = new ArrayDeque<>();
    private boolean takeCharacters = false;
    private String buffer = null;
    private boolean ignoreElements = false;

    public XMLParser(File file) throws FileNotFoundException {
        parse(file);
    }

    public Document getDocument() {
        return document;
    }

    private void parse(File file) throws FileNotFoundException {
        try {
            // Create the SAX parser
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            // Get the XML reader
            XMLReader xmlReader = saxParser.getXMLReader();

            // Set a new EntityResolver to ignore all external entities
            // (otherwise it will look for a ipe.dtd file)
            EntityResolver ignoreExternalEntities = new EntityResolver() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(new StringReader(""));
                }
            };
            xmlReader.setEntityResolver(ignoreExternalEntities);

            // Set the handler with callback functions to parse the input file
            Handler handler = new Handler();
            xmlReader.setContentHandler(handler);

            // Create an input source capable of dealing with UTF-8 encoding
            InputStream inputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            InputSource is = new InputSource(inputStreamReader);
            is.setEncoding("UTF-8");

            try {
                xmlReader.parse(is);
            } catch (IOException ex) {
                System.err.println("IO error parsing the input file.");
            }
        } catch (ParserConfigurationException ex) {
            System.err.println("Error setting up the XML parser.");
        } catch (SAXException ex) {
            System.err.println("Error parsing the input file.");
        } catch (UnsupportedEncodingException ex) {
            System.err.println("File encoding not supported by the XML parser.");
        }
    }

    private void startIPEDocument(Attributes attributes) {
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "version":
                    document.setVersion(Integer.parseInt(value));
                    break;
                case "creator":
                    document.setCreator(value);
                    break;
            }
        }
    }

    private void startStyleSheet(Attributes attributes) {
        openStyleSheet = new StyleSheet();
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "name":
                    openStyleSheet.setName(value);
                    break;
            }
        }
    }

    private void startGradient(Attributes attributes) {
        String gradientName = null;
        boolean isAxial = true;
        Boolean extend = null;
        double[] coords = null;
        MatrixAttribute matrix = null;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "name":
                    gradientName = value;
                    break;
                case "type":
                    if (value.equals("radial")) {
                        isAxial = false;
                    }
                    break;
                case "extend":
                    if (value.equals("yes")) {
                        extend = true;
                    } else {
                        extend = false;
                    }
                    break;
                case "coords":
                    coords = parseDoubleArray(value);
                    break;
                case "matrix":
                    matrix = this.parseMatrixAttribute(value);
                    break;
            }
        }
        if (isAxial) {
            Gradient.Axial axialGradient = new Gradient.Axial(gradientName);
            axialGradient.setFirstEndpoint(coords[0], coords[1]);
            axialGradient.setSecondEndpoint(coords[2], coords[3]);
            openGradient = axialGradient;
        } else {
            Gradient.Radial radialGradient = new Gradient.Radial(gradientName);
            radialGradient.setFirstCircle(coords[0], coords[1], coords[2]);
            radialGradient.setSecondCircle(coords[3], coords[4], coords[5]);
            openGradient = radialGradient;
        }
        openGradient.setExtend(extend);
        openGradient.setMatrix(matrix);
    }

    private void startStop(Attributes attributes) {
        double offset = 0.0;
        ColorAttribute.RGB colorAttribute = null;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            if (name.equals("offset")) {
                offset = Double.parseDouble(value);
            } else if (name.equals("color")) {
                colorAttribute = (ColorAttribute.RGB) parseColorAttribute(value);
            }
        }
        openGradient.addStop(offset, colorAttribute);
    }

    private void startPen(Attributes attributes) {
        String symbolicName = null;
        Float width = null;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "name":
                    symbolicName = value;
                    break;
                case "value":
                    width = Float.parseFloat(value);
                    break;
            }
        }
        PenAttribute.Real penAttribute = new PenAttribute.Real(width);
        SymbolicPen symbolicPen = new SymbolicPen(symbolicName, penAttribute);
        openStyleSheet.addSymbolicPen(symbolicPen);
    }

    private void startColor(Attributes attributes) {
        String symbolicName = null;
        String[] components = null;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "name":
                    symbolicName = value;
                    break;
                case "value":
                    components = value.trim().split("\\s+");
                    break;
            }
        }
        if (components == null) {
            return;
        }
        SymbolicColor symbolicColor = null;
        if (components.length == 1) {
            float intensity = Float.parseFloat(components[0]);
            ColorAttribute.Gray colorAttribute = new ColorAttribute.Gray(intensity);
            symbolicColor = new SymbolicColor(symbolicName, colorAttribute);
        } else if (components.length == 3) {
            float red = Float.parseFloat(components[0]);
            float green = Float.parseFloat(components[1]);
            float blue = Float.parseFloat(components[2]);
            ColorAttribute.RGB colorAttribute = new ColorAttribute.RGB(red, green, blue);
            symbolicColor = new SymbolicColor(symbolicName, colorAttribute);
        }
        openStyleSheet.addSymbolicColor(symbolicColor);
    }

    private void startLayer(Attributes attributes) {
        String layerName = null;
        Boolean editable = null;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "name":
                    layerName = value;
                    break;
                case "edit":
                    if (value.equals("yes")) {
                        editable = true;
                    } else {
                        editable = false;
                    }
                    break;
            }
        }
        Layer layer = new Layer(layerName);
        if (editable != null) {
            layer.setEditable(editable);
        }
        document.addLayer(layer);
    }

    private void startPath(Attributes attributes) {
        openPath = new Path();
        buffer = new String();
        takeCharacters = true;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "layer":
                    openPath.setLayer(value);
                    break;
                case "matrix":
                    openPath.setMatrix(parseMatrixAttribute(value));
                    break;
                case "transformations":
                    switch (value) {
                        case "affine":
                            openPath.setTransformations(IpeObject.Transformations.AFFINE);
                            break;
                        case "rigid":
                            openPath.setTransformations(IpeObject.Transformations.RIGID);
                            break;
                        case "translations":
                            openPath.setTransformations(IpeObject.Transformations.TRANSLATIONS);
                            break;
                    }
                    break;
                case "stroke":
                    ColorAttribute strokeColor = parseColorAttribute(value);
                    openPath.setStrokeColor(strokeColor);
                    break;
                case "fill":
                    ColorAttribute fillColor = parseColorAttribute(value);
                    openPath.setFillColor(fillColor);
                    break;
                case "pen":
                    PenAttribute pen = parsePenAttribute(value);
                    openPath.setPen(pen);
                    break;
                case "fillrule":
                    if (value.equals("wind")) {
                        openPath.setFillRule(Path.FillRule.WIND);
                    } else if (value.equals("eofill")) {
                        openPath.setFillRule(Path.FillRule.EOFILL);
                    }
                    break;
            }
        }
    }

    private void startText(Attributes attributes) {
        openText = new Text();
        buffer = new String();
        takeCharacters = true;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "layer":
                    openText.setLayer(value);
                    break;
                case "matrix":
                    openText.setMatrix(parseMatrixAttribute(value));
                    break;
                case "transformations":
                    switch (value) {
                        case "affine":
                            openText.setTransformations(IpeObject.Transformations.AFFINE);
                            break;
                        case "rigid":
                            openText.setTransformations(IpeObject.Transformations.RIGID);
                            break;
                        case "translations":
                            openText.setTransformations(IpeObject.Transformations.TRANSLATIONS);
                            break;
                    }
                    break;
                case "stroke":
                    ColorAttribute strokeColor = parseColorAttribute(value);
                    openText.setStrokeColor(strokeColor);
                    break;
                case "type":
                    if (value.equals("label")) {
                        openText.setType(Text.Type.LABEL);
                    } else if (value.equals("minipage")) {
                        openText.setType(Text.Type.MINIPAGE);
                    }
                    break;
                case "pos":
                    PointAttribute position = parsePointAttribute(value);
                    openText.setPosition(position);
                    break;
                case "width":
                    double width = Double.parseDouble(value);
                    openText.setWidth(width);
                    break;
                case "height":
                    double height = Double.parseDouble(value);
                    openText.setHeight(height);
                    break;
                case "depth":
                    double depth = Double.parseDouble(value);
                    openText.setDepth(depth);
                    break;
                case "valign":
                    switch (value) {
                        case "top":
                            openText.setVerticalAlignment(Text.VerticalAlignment.TOP);
                            break;
                        case "bottom":
                            openText.setVerticalAlignment(Text.VerticalAlignment.BOTTOM);
                            break;
                        case "center":
                            openText.setVerticalAlignment(Text.VerticalAlignment.CENTER);
                            break;
                        case "baseline":
                            openText.setVerticalAlignment(Text.VerticalAlignment.BASELINE);
                            break;
                    }
                    break;
                case "halign":
                    switch (value) {
                        case "left":
                            openText.setHorizontalAlignment(Text.HorizontalAlignment.LEFT);
                            break;
                        case "right":
                            openText.setHorizontalAlignment(Text.HorizontalAlignment.RIGHT);
                            break;
                        case "center":
                            openText.setHorizontalAlignment(Text.HorizontalAlignment.CENTER);
                            break;
                    }
                    break;
            }
        }
    }

    private void startGroup(Attributes attributes) {
        Group current = new Group();
        openGroups.push(current);
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getQName(i);
            String value = attributes.getValue(i);
            switch (name) {
                case "layer":
                    current.setLayer(value);
                    break;
                case "matrix":
                    current.setMatrix(parseMatrixAttribute(value));
                    break;
                case "transformations":
                    switch (value) {
                        case "affine":
                            current.setTransformations(IpeObject.Transformations.AFFINE);
                            break;
                        case "rigid":
                            current.setTransformations(IpeObject.Transformations.RIGID);
                            break;
                        case "translations":
                            current.setTransformations(IpeObject.Transformations.TRANSLATIONS);
                            break;
                    }
                    break;
            }
        }
    }

    private void endStyleSheet() {
        document.addStyleSheet(openStyleSheet);
        openStyleSheet = null;
    }

    private void endGradient() {
        openStyleSheet.addGradient(openGradient);
        openGradient = null;
    }

    private void endPath() {
        String[] components = buffer.trim().split("\\s+");
        ArrayDeque<Double> values = new ArrayDeque<>();
        for (String s : components) {
            char first = s.charAt(0);
            if (isNumeric(first)) {
                double d = Double.parseDouble(s);
                values.push(d);
            } else {
                double x, y;
                switch (first) {
                    case 'm':
                        y = values.pop();
                        x = values.pop();
                        openPath.moveTo(x, y);
                        break;
                    case 'l':
                        y = values.pop();
                        x = values.pop();
                        openPath.lineTo(x, y);
                        break;
                    case 'h':
                        openPath.closePath();
                        break;
                }
            }
        }

        Group current = openGroups.peek();
        if (current == null) {
            document.addObject(openPath);
        } else {
            current.addObject(openPath);
        }
        takeCharacters = false;
        openPath = null;
        buffer = null;
    }

    private void endText() {
        openText.setText(buffer);
        Group current = openGroups.peek();
        if (current == null) {
            document.addObject(openText);
        } else {
            current.addObject(openText);
        }
        takeCharacters = false;
        openText = null;
        buffer = null;
    }

    private void endGroup() {
        Group finishedGroup = openGroups.pop();
        Group current = openGroups.peek();
        if (current == null) {
            document.addObject(finishedGroup);
        } else {
            current.addObject(finishedGroup);
        }
    }

    private ColorAttribute parseColorAttribute(String string) {
        char first = string.charAt(0);
        if (Character.isJavaIdentifierStart(first)) {
            switch (string) {
                case "black":
                    return new ColorAttribute.Black();
                case "white":
                    return new ColorAttribute.White();
                default:
                    SymbolicColor symbolicColor = document.lookUpSymbolicColor(string);
                    return new ColorAttribute.Symbolic(symbolicColor);
            }
        } else if (isNumeric(first)) {
            String[] components = string.trim().split("\\s+");
            if (components.length == 1) {
                float intensity = Float.parseFloat(components[0]);
                return new ColorAttribute.Gray(intensity);
            } else if (components.length == 3) {
                float red = Float.parseFloat(components[0]);
                float green = Float.parseFloat(components[1]);
                float blue = Float.parseFloat(components[2]);
                return new ColorAttribute.RGB(red, green, blue);
            }
        }
        return null;
    }

    private MatrixAttribute parseMatrixAttribute(String string) {
        String[] components = string.trim().split("\\s+");
        double[] values = new double[6];
        for (int i = 0; i < 6; i++) {
            values[i] = Double.parseDouble(components[i]);
        }
        return new MatrixAttribute(values);
    }

    private PenAttribute parsePenAttribute(String string) {
        char first = string.charAt(0);
        if (Character.isJavaIdentifierStart(first)) {
            SymbolicPen symbolicPen = document.lookUpSymbolicPen(string);
            return new PenAttribute.Symbolic(symbolicPen);
        } else if (isNumeric(first)) {
            float width = Float.parseFloat(string);
            return new PenAttribute.Real(width);
        }
        return null;
    }

    private PointAttribute parsePointAttribute(String string) {
        String[] components = string.trim().split("\\s+");
        if (components.length == 2) {
            double x = Double.parseDouble(components[0]);
            double y = Double.parseDouble(components[1]);
            return new PointAttribute(x, y);
        }
        return null;
    }

    private double[] parseDoubleArray(String string) {
        String[] components = string.trim().split("\\s+");
        double[] values = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            values[i] = Double.parseDouble(components[i]);
        }
        return values;
    }

    private boolean isNumeric(char c) {
        return Character.isDigit(c) | c == '.' | c == '+' | c == '-';
    }

    private class Handler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equals("symbol")) {
                ignoreElements = true;
            }
            if (ignoreElements) {
                return;
            }
            switch (qName) {
                case "ipe":
                    startIPEDocument(attributes);
                    break;
                case "ipestyle":
                    startStyleSheet(attributes);
                    break;
                case "gradient":
                    startGradient(attributes);
                    break;
                case "stop":
                    startStop(attributes);
                    break;
                case "pen":
                    startPen(attributes);
                    break;
                case "color":
                    startColor(attributes);
                    break;
                case "layer":
                    startLayer(attributes);
                    break;
                case "path":
                    startPath(attributes);
                    break;
                case "text":
                    startText(attributes);
                    break;
                case "group":
                    startGroup(attributes);
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (takeCharacters) {
                buffer = buffer.concat(new String(ch, start, length));
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (qName.equals("symbol")) {
                ignoreElements = false;
                return;
            }
            if (ignoreElements) {
                return;
            }
            switch (qName) {
                case "ipestyle":
                    endStyleSheet();
                    break;
                case "gradient":
                    endGradient();
                    break;
                case "path":
                    endPath();
                    break;
                case "text":
                    endText();
                    break;
                case "group":
                    endGroup();
                    break;
            }
        }
    }
}
