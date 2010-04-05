package ucar.unidata;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.repository.Constants;
import ucar.unidata.repository.RequestUrl;
import ucar.unidata.repository.client.RepositoryClient;
import ucar.unidata.ui.HttpFormEntry;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

@SuppressWarnings("nls")
public class Main implements Constants {

    public static void main( String[] args ) throws Exception {

        /*
         * upload netcdf
         */
        // uploadFileToRamadda("79.14.80.158", 8080, "/repository", "moovida", "lizard", "water.nc",
        // "first test file", "Main Repository/morpheo", "/home/moovida/TMP/ramadda/water.nc");

        /*
         * upload png
         */
         uploadFileToRamadda("79.14.80.158", 8080, "/repository", "moovida", "lizard",
         "reprojtif.png", "image test", "png", "morpheo/documents",
         "/home/moovida/Desktop/reprojtif.png");

        /*
         * browse
         */
        // http://<your server>/repository/entry/show?entryid=<entry id>
//        String entryId = "c827f1fd-4c0e-4f19-bfc6-7f280176b4c9";
//        File outputFile = new File("/home/moovida/TMP/reprojtif.png");
//        downloadFileFromRamadda("79.14.80.158", 8080, "/repository", "moovida", "lizard", entryId,
//                outputFile);

    }

    public static void test( String host, int port, String base, String user, String passwd )
            throws Exception {
        RepositoryClient client = new RepositoryClient(host, port, base, user, passwd);
        String[] msg = {""};
        if (client.isValidSession(true, msg)) {
            System.err.println("Valid session");
        } else {
            System.err.println("Invalid session:" + msg[0]);
            return;
        }

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        postEntries.add(HttpFormEntry.hidden(ARG_SESSIONID, client.getSessionId()));
        postEntries.add(HttpFormEntry.hidden(ARG_OUTPUT, "xml.xml"));
        // RequestUrl URL_SEARCH_BROWSE = new RequestUrl(client, "/search/browse", "Browse");
        // RequestUrl URL_LIST_SHOW = new RequestUrl(client, "/list/show");
        RequestUrl URL_ENTRY_SHOW = new RequestUrl(client, "/entry/show");
        RequestUrl URL = URL_ENTRY_SHOW;

        String[] result = client.doPost(URL, postEntries);
        if (result[0] != null) {
            System.err.println("Error:" + result[0]);
            return;
        }

        System.out.println("result:" + result[1]);
        Element response = XmlUtil.getRoot(result[1]);
        NodeList childNodes = response.getChildNodes();
        int length = childNodes.getLength();
        for( int i = 0; i < length; i++ ) {
            Node item = childNodes.item(i);
            String body = XmlUtil.getChildText(item).trim();
            System.out.println(body);
            System.out.println("--");
        }

    }

    public static void downloadFileFromRamadda( String host, int port, String base, String user,
            String passwd, String entryId, File outputFile ) throws Exception {
        RepositoryClient client = new RepositoryClient(host, port, base, user, passwd);
        String[] msg = {""};
        if (client.isValidSession(true, msg)) {
            System.err.println("Valid session");
        } else {
            System.err.println("Invalid session:" + msg[0]);
            return;
        }

//        client.findById(entryId, outputFile);

    }

//    uploadFileToRamadda("79.14.80.158", 8080, "/repository", "moovida", "lizard",
//            "reprojtif.png", "image test", "png", "morpheo/documents",
//            "/home/moovida/Desktop/reprojtif.png");
    public static void uploadFileToRamadda( String host, int port, String base, String user,
            String passwd, String entryName, String entryDescription, String type, String parent,
            String filePath ) throws Exception {

        RepositoryClient client = new RepositoryClient(host, port, base, user, passwd);
        String[] msg = {""};
        if (client.isValidSession(true, msg)) {
            System.err.println("Valid session");
        } else {
            System.err.println("Invalid session:" + msg[0]);
            return;
        }

        Document doc = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, TAG_ENTRIES, null, new String[]{});
        Element entryNode = XmlUtil.create(doc, TAG_ENTRY, root, new String[]{});

        /*
         * name
         */
        entryNode.setAttribute(ATTR_NAME, entryName);
        /*
         * description
         */
        Element descNode = XmlUtil.create(doc, TAG_DESCRIPTION, entryNode);
        descNode.appendChild(XmlUtil.makeCDataNode(doc, entryDescription, false));
        /*
         * type
         */
        if (type != null) {
            entryNode.setAttribute(ATTR_TYPE, type);
        }
        /*
         * parent
         */
        entryNode.setAttribute(ATTR_PARENT, parent);
        /*
         * file
         */
        File file = new File(filePath);
        entryNode.setAttribute(ATTR_FILE, IOUtil.getFileTail(filePath));
        /*
         * addmetadata
         */
        entryNode.setAttribute(ATTR_ADDMETADATA, "true");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);

        /*
         * write the xml definition into the zip file
         */
        String xml = XmlUtil.toString(root);
        zos.putNextEntry(new ZipEntry("entries.xml"));
        byte[] bytes = xml.getBytes();
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        /*
         * add also the file
         */
        String file2string = file.toString();
        zos.putNextEntry(new ZipEntry(IOUtil.getFileTail(file2string)));
        bytes = IOUtil.readBytes(new FileInputStream(file));
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();

        zos.close();
        bos.close();

        List<HttpFormEntry> postEntries = new ArrayList<HttpFormEntry>();
        postEntries.add(HttpFormEntry.hidden(ARG_SESSIONID, client.getSessionId()));
        postEntries.add(HttpFormEntry.hidden(ARG_RESPONSE, RESPONSE_XML));
        postEntries.add(new HttpFormEntry(ARG_FILE, "entries.zip", bos.toByteArray()));

        RequestUrl URL_ENTRY_XMLCREATE = new RequestUrl(client, "/entry/xmlcreate");
        String[] result = client.doPost(URL_ENTRY_XMLCREATE, postEntries);
        if (result[0] != null) {
            System.err.println("Error:" + result[0]);
            return;
        }

        System.err.println("result:" + result[1]);
        Element response = XmlUtil.getRoot(result[1]);

        String body = XmlUtil.getChildText(response).trim();
        if (client.responseOk(response)) {
            System.err.println("OK:" + body);
        } else {
            System.err.println("Error:" + body);
        }

    }
}
