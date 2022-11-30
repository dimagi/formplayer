package org.commcare.formplayer.junit;

import static org.hamcrest.Condition.matched;
import static org.hamcrest.Condition.notMatched;

import static javax.xml.xpath.XPathConstants.STRING;

import org.hamcrest.Condition;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Applies a Matcher to a given XML String, specified by an XPath expression.
 */
public class HasXPath extends TypeSafeDiagnosingMatcher<String> {

    private final Matcher<String> valueMatcher;
    private final String xpathString;
    private final XPathExpression compiledXPath;

    private static final Condition.Step<Object,String> NODE_EXISTS = nodeExists();

    private HasXPath(String xPathExpression, Matcher<String> valueMatcher) {
        this.compiledXPath = compiledXPath(xPathExpression, null);
        this.xpathString = xPathExpression;
        this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(String item, Description mismatch) {
        return evaluated(item, mismatch)
                .and(NODE_EXISTS)
                .matching(valueMatcher);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an XML document string with XPath ").appendText(xpathString);
        if (valueMatcher != null) {
            description.appendText(" ").appendDescriptionOf(valueMatcher);
        }
    }

    /**
     * Creates a matcher of {@link java.lang.String}s that matches when the examined string has a value at the
     * specified <code>xPath</code> that satisfies the specified <code>valueMatcher</code>.
     * For example:
     * <pre>assertThat(xmlString, hasXPath("/root/something[2]/cheese", equalTo("Cheddar")))</pre>
     *
     * @param xPath
     *     the target xpath
     * @param valueMatcher
     *     matcher for the value at the specified xpath
     */
    public static Matcher<String> hasXPath(String xPath, Matcher<String> valueMatcher) {
        return new HasXPath(xPath, valueMatcher);
    }

    private Condition<Object> evaluated(String item, Description mismatch) {
        Document document;
        try {
            document = parseXml(item);
        } catch (Exception e) {
            mismatch.appendText(e.getMessage());
            return notMatched();
        }

        try {
            return matched(compiledXPath.evaluate(document, STRING), mismatch);
        } catch (XPathExpressionException e) {
            mismatch.appendText(e.getMessage());
        }
        return notMatched();
    }

    private static XPathExpression compiledXPath(String xPathExpression, NamespaceContext namespaceContext) {
        try {
            final XPath xPath = XPathFactory.newInstance().newXPath();
            if (namespaceContext != null) {
                xPath.setNamespaceContext(namespaceContext);
            }
            return xPath.compile(xPathExpression);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid XPath : " + xPathExpression, e);
        }
    }

    private static Condition.Step<Object, String> nodeExists() {
        return (value, mismatch) -> {
            if (value == null) {
                mismatch.appendText("xpath returned no results.");
                return notMatched();
            }
            return matched(String.valueOf(value), mismatch);
        };
    }

    protected Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new ByteArrayInputStream(xml.getBytes()));
        return documentBuilder.parse(inputSource);
    }
}
