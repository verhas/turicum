package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 * Format an XML string.
 */
public class XmlFormat implements TuriFunction {
    @Override
    public String name() {
        return "xml_format";
    }

    private static final PrintStream NULL_ERR = new PrintStream(new OutputStream() {
        public void write(int b) {
            // Do nothing
        }
    });


    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.oneArg(name(), args);

        final var input = "" + args[0];

        final var savedErr = System.err;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(input)));
            System.setErr(NULL_ERR);// format document sometimes vomits error to System.err when the XML is not well-formed
            return formatDocument(doc, 4);
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            throw new ExecutionException("There was an XML exception formatting XML", e);
        } finally {
            System.setErr(savedErr);
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
     * XML file. On the other hand when we write them back to a file formatted, then they are treated as first class
     * citizen nodes, that deserve their separate lines and indentation.
     * <p>
     * This way reading an XML file and writing back inserts a new new-line for one already existing, unless we delete
     * these before formatting.
     *
     * @param doc the document to remove the blank text nodes from
     */
    private static void trimDocument(Document doc) {
        final var root = doc.getDocumentElement();
        trimNode(root);
    }

    /**
     *
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
