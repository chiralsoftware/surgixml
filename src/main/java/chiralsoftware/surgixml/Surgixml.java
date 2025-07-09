package chiralsoftware.surgixml;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileOutputStream;
import java.util.List;

import static java.lang.System.err;

@Command(name = "surgixml", mixinStandardHelpOptions = true, version = "1.0",
        description = "Edits or inserts XML based on XPath and attribute rules.")
public final class Surgixml implements Runnable {

    @Option(names = "--file", required = true, description = "Path to the XML file.")
    String xmlFile;

    // example:
    // --edit-attribute="/Server/Service/Connector[@port='8080']@port=80"
    @Option(names = "--edit-attribute", arity = "1..*", description = "Edit in form: xpath@attr=value")
    List<String> edits;

    // example:
    //  --insert-after-head='/Server/Service[@name="Catalina"]:<Connector port="99"><!-- hello --></Connector>\n'
    @Option(names = "--insert-after-head", arity = "1..*", description = "Insert in form: xpath:xmlFragment")
    List<String> insertAfterHead;
    
    @Option(names = "--insert-after-element", arity= "1..*", description = "Insert in form: xpath:xmlFragment")
    List<String> insertAfterElement;

    @Option(names = "--insert-before-element", arity= "1..*", description = "Insert in form: xpath:xmlFragment")
    List<String> insertBeforeElement;

    @Option(names = "--insert-after-location", arity= "1..*", description = "Insert in form: xpath:xmlFragment . Use this to insert after elements such as comments")
    List<String> insertAfterLocation;


    @Override
    public void run() {

        final VTDGen vg = new VTDGen();
        if (!vg.parseFile(xmlFile, false)) {
            err.println("Failed to parse XML.");
            return;
        }
        final VTDNav vn = vg.getNav();
        final AutoPilot ap = new AutoPilot(vn);
        try {
            final XMLModifier xm = new XMLModifier(vn); // this is a buffer of accumulated changes and is valid until .output() is called

            if (edits != null) {
                for (String edit : edits) {
                    final AttributeEdit attributeEdit = AttributeEdit.parse(edit);

                    ap.selectXPath(attributeEdit.xpath());
                    int index;
                    while((index = ap.evalXPath()) != -1) {
                        final int attrIndex = vn.getAttrVal(attributeEdit.attribute());
                        if(attrIndex != -1)
                            xm.updateToken(attrIndex, attributeEdit.newValue());
                    }
                }
            }
            if(insertAfterHead != null) {
                for (String insert : insertAfterHead) {
                    final ElementInsert elementInsert = ElementInsert.parse(insert);
                    ap.selectXPath(elementInsert.xpath());
                    if(ap.evalXPath() != -1) {
                        xm.insertAfterHead(elementInsert.value());
                        //or use ap.selectXPath("/Server/Service/Connector[last()]"); and xm.insertAfterElement
                    }
                }
            }
            
            // use this xpath to insert after the connector port 8080:
            // "//Connector[@port='8080']"
            if(insertAfterElement != null) {
                for (String insert : insertAfterElement) {
                    final ElementInsert elementInsert = ElementInsert.parse(insert);
                    ap.selectXPath(elementInsert.xpath());
                    if(ap.evalXPath() != -1) {
                        xm.insertAfterElement(elementInsert.value());
                    }
                }
            }

            if(insertBeforeElement != null) {
                for (String insert : insertBeforeElement) {
                    final ElementInsert elementInsert = ElementInsert.parse(insert);
                    ap.selectXPath(elementInsert.xpath());
                    if(ap.evalXPath() != -1) {
                        xm.insertBeforeElement(elementInsert.value());
                    }
                }
            }

            if(insertAfterLocation != null) {
                for (String insert : insertAfterLocation) {
                    final ElementInsert elementInsert = ElementInsert.parse(insert);
                    ap.selectXPath(elementInsert.xpath());
                    final int tokenOffset = ap.evalXPath();
//                    final String commentText = vn.toNormalizedString(tokenOffset);
//                    out.println("Here is the comment text i found:");
//                    out.println(commentText);

                    if(tokenOffset != -1) {
                        final int offset = vn.getTokenOffset(tokenOffset) + vn.getTokenLength(tokenOffset);
                        xm.insertBytesAt(offset + 3, elementInsert.value().getBytes());
                    }
                }
            }

            final FileOutputStream fos = new FileOutputStream(xmlFile);
            xm.output(fos);
            fos.close();
        } catch(Exception e) {
            e.printStackTrace(err);
        }
    }

}
