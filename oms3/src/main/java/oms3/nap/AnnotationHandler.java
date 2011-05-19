package oms3.nap;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Annotation Handler.
 *
 * Implement this interface to handle annotations. The processor will
 * call this handler once with start(), then handle(..) n-times 
 * for n annotated lines,
 * and finally done() when done with this file.
 *
 * @author od
 */
public interface AnnotationHandler {

    public static final String VALUE = "value";

    /**
     * Called by the processor when starting processing annotations for a file.
     *
     * @param file the file that is processed as String.
     */
    void start(String src);

    /**
     * Handles a single line of annotated code.
     *
     * @param annValue      annotations KVPs in a map. It is empty if there
     *                       is no annotation argument (tagging annotation), annValue.get("value") will
     *                       return a single value argument.
     * @param rcLine    the source line that follows the annotations, if there
     *                       is none (e.g. another annotation follows), the value will be null.
     */
    void handle(Map<String, Map<String, String>> ann, String srcLine);

    /**
     * Logging during parsing
     * @param msg
     */
    void log(String msg);

    /**
     * Done with file processing.
     * @throws Exception 
     */
    void done() throws Exception;
}
