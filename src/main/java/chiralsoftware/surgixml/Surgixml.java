package chiralsoftware.surgixml;

import com.ximpleware.AutoPilot;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.FileOutputStream;
import java.util.List;

import static com.ximpleware.VTDGen.*;
import static java.lang.System.err;
import static java.lang.System.out;

@Command(name = "surgixml", mixinStandardHelpOptions = true, version = "1.0",
        description = "Edits or inserts XML based on XPath and attribute rules.")
public final class Surgixml implements Runnable {

    @Option(names = "--file", required = true, description = "Path to the XML file.")
    String xmlFile;

    // example:
    // --edit-attribute="/Server/Service/Connector[@port='8080']@port=80"
    @Option(names = "--edit-attribute", arity = "1..*", description = "Edit in form: xpath@attr=value")
    List<String> edits;

    @Option(names = "--add-attribute", arity = "1..*", description = "Add an attribute to a node")
    List<String> adds;

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

    @Option(names = "--namespace", arity = "1..*", description = "Define a namespace")
    List<String> namespaces;

    @Option(names = "--separator", arity = "1", description = "define a separator between xpath and value",
    defaultValue = ":")
    String separator;


    @Override
    public void run() {

        final VTDGen vg = new VTDGen();
        if (!vg.parseFile(xmlFile, namespaces != null && ! namespaces.isEmpty())) {
            err.println("Failed to parse XML.");
            return;
        }
        final VTDNav vn = vg.getNav();
        final AutoPilot ap = new AutoPilot(vn);
        if(namespaces != null) {
            for (String namespace : namespaces) {
                final String prefix = namespace.substring(0, namespace.indexOf(":"));
                final String url = namespace.substring(namespace.indexOf(":") + 1);
                ap.declareXPathNameSpace(prefix, url);
//                System.out.println("added prefix: " + prefix + " and url: " + url);
            }
        }
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
                    final ElementInsert elementInsert = ElementInsert.parse(insert, separator);
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
                    final ElementInsert elementInsert = ElementInsert.parse(insert, separator);
                    ap.selectXPath(elementInsert.xpath());
                    if(ap.evalXPath() != -1) {
                        xm.insertAfterElement(elementInsert.value());
                    }
                }
            }

            if(insertBeforeElement != null) {
                for (String insert : insertBeforeElement) {
                    final ElementInsert elementInsert = ElementInsert.parse(insert, separator);
                    ap.selectXPath(elementInsert.xpath());
                    if(ap.evalXPath() != -1) {
                        xm.insertBeforeElement(elementInsert.value());
                    }
                }
            }

            if(insertAfterLocation != null) {
                for (String insert : insertAfterLocation) {
                    final ElementInsert elementInsert = ElementInsert.parse(insert, separator);
                    ap.selectXPath(elementInsert.xpath());
                    final int tokenOffset = ap.evalXPath();
                    if(tokenOffset != -1) {
                        final String commentText = vn.toNormalizedString(tokenOffset);
                        final int tokenType = vn.getTokenType(tokenOffset);
                        final int offset = vn.getTokenOffset(tokenOffset) + vn.getTokenLength(tokenOffset);
//                        out.println("Here is the text i found with type: " + tokenType +
//                                " which has length: " + vn.getTokenLength(tokenOffset) + " and is at character: " + offset);
//                        out.println(commentText);
                        final int extraOffset = switch (tokenType) {
                            case TOKEN_COMMENT -> 3;
                            case TOKEN_STARTING_TAG -> 1;
                            case TOKEN_PI_NAME -> 2;
                            default -> 1;
                        };

                        xm.insertBytesAt(offset + extraOffset, elementInsert.value().getBytes());
                    }
                }
            }

            // this works exactly like insertAfterLocation except it's going to find the last
            // it finds the end of the list of attributes and adds there.
            if(adds != null) {
                for (String add : adds) {
                    final ElementInsert elementInsert = ElementInsert.parse(add, separator);
                    ap.selectXPath(elementInsert.xpath());
                    final int tokenOffset = ap.evalXPath();
                    if(tokenOffset != -1) {
                        final String commentText = vn.toNormalizedString(tokenOffset);
                        final int tokenType = vn.getTokenType(tokenOffset);
                        if(tokenType == TOKEN_STARTING_TAG) {
                            int lastAttrOffset = -1;
                            for(int i = tokenOffset + 1; i < vn.getTokenCount(); i++) {
//                                out.println("i = " + i );
                                final int nextType = vn.getTokenType(i);
//                                out.println("i = " + i + " and nextType= " + nextType);
                                if(nextType == TOKEN_ATTR_NAME || nextType == TOKEN_ATTR_VAL)
                                    lastAttrOffset = vn.getTokenOffset(i) + vn.getTokenLength(i);
                                else break;
                            }

                            if(lastAttrOffset > 0)
                                xm.insertBytesAt(lastAttrOffset + 1, elementInsert.value().getBytes());
                        }
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
