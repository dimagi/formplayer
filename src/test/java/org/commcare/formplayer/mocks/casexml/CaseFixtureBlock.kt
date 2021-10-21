package org.commcare.formplayer.mocks.casexml

import org.w3c.dom.Document
import java.io.StringWriter
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Representation of a case for serialization as a case fixture element:
 *
 * ```
 * <results id="case">
 *    <case case_id="" case_type="" owner_id="" status="">
 *      <case_name/>
 *      <date_opened/>
 *      <last_modified/>
 *      <case_property />
 *      <index>
 *          <a12345 case_type="" relationship="" />
 *      </index>
 *      <attachment>
 *          <a12345 />
 *      </attachment>
 *    </case>
 *    <case>
 *    ...
 *    </case>
 * </results>
 * ```
 *
 * Usage:
 * ```java
 * val block: CaseFixtureBlock = CaseFixtureBlock.Builder("case", "case_id")
 *      .property("case_name", "name")
 *      .property("other", "any")
 *      .build()
 * val fixture: String = CaseFixtureResult.blocksToString(block, ...)
 * ```
 *
 * See https://github.com/dimagi/commcare-core/wiki/casedb
 *
 */
class CaseFixtureBlock private constructor(
        val caseType: String,
        val caseId: String,
        val ownerId: String,
        val props: Map<String, String>,
) {
    val status: String = "open"

    class Builder @JvmOverloads constructor(
            private var caseType: String,
            private var caseId: String? = null,
            private var ownerId: String? = null,
    ) {
        private val props: MutableMap<String, String> = mutableMapOf();

        fun property(name: String, value: String) = apply { this.props[name] = value }

        fun build(): CaseFixtureBlock {
            return CaseFixtureBlock(
                caseType,
                if (caseId == null) UUID.randomUUID().toString() else caseId!!,
                if (ownerId == null) UUID.randomUUID().toString() else ownerId!!,
                props
            )
        }
    }
}


class CaseFixtureResult() {

    companion object {
        @JvmStatic fun blocksToXml(vararg blocks: CaseFixtureBlock): Document? {
            val docFactory = DocumentBuilderFactory.newInstance();
            val docBuilder = docFactory.newDocumentBuilder();

            val doc = docBuilder.newDocument();

            val results = doc.createElement("results");
            results.setAttribute("id", "case")
            doc.appendChild(results);

            for (block in blocks) {
                val case = doc.createElement("case")
                results.appendChild(case)
                case.setAttribute("case_id", block.caseId)
                case.setAttribute("case_type", block.caseType)
                case.setAttribute("owner_id", block.ownerId)
                case.setAttribute("status", block.status)
                for ((name, value) in block.props) {
                    print(name)
                    print(value)
                    val prop = doc.createElement(name)
                    prop.textContent = value
                    case.appendChild(prop)
                }
            }
            return doc;
        }

        @JvmStatic fun blocksToString(vararg blocks: CaseFixtureBlock): String {
            val xml = blocksToXml(*blocks)

            //write the content into xml file
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

            val writer = StringWriter()
            transformer.transform(DOMSource(xml), StreamResult(writer))
            return writer.toString()
        }
    }
}
