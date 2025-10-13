package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
/*snippet builtin0530

end snippet */

/**
 * Format an XML string.
 */
public class XmlFormat implements TuriFunction {

    /**
     * A static final instance of an {@link ErrorHandler} that suppresses warnings
     * and throws exceptions on errors and fatal errors when processing XML documents.
     * <p>
     * Used to suppress non-critical warnings during XML formatting while maintaining proper error handling.
     * <li> Warnings are logged silently and ignored.
     * <li> Errors and fatal errors throw the associated {@link SAXParseException}.
     */
    private static final ErrorHandler SILENT_HANDLER = new ErrorHandler() {
        @Override
        public void warning(SAXParseException exception) {
            // Ignore warnings
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    };


    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);

        final var input = "" + arg;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(SILENT_HANDLER);
            Document doc = db.parse(new InputSource(new StringReader(input)));
            return formatDocument(doc, 4);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            throw new ExecutionException("There was an XML exception formatting XML", e);
        }
    }

    public static String formatDocument(Document doc, int tabsize) throws TransformerException {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "" + tabsize);
        Writer out = new StringWriter();
        trimDocument(doc);
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return out.toString();
    }

    /**
     * Remove the blank text nodes from the XML. These are the nodes that result from the source formatting of the
     * XML file. On the other hand, when we write them back to a file, formatted, then they are treated as first-class
     * citizen nodes, that deserve their separate lines and indentation.
     * <p>
     * This way, reading an XML file and writing back inserts a new new-line for one already existing, unless we delete
     * these before formatting.
     *
     * @param doc the document to remove the blank text nodes from
     */
    private static void trimDocument(Document doc) {
        final var root = doc.getDocumentElement();
        trimNode(root);
    }

    /**
     * @param node the node to remove the blank text nodes from recursively
     */
    private static void trimNode(Node node) {
        final var children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getTextContent().trim().isEmpty()) {
                    child.setTextContent("");
                }
            } else {
                trimNode(child);
            }
        }
    }
}
