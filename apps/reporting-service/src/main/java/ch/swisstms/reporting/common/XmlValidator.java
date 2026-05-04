package ch.swisstms.reporting.common;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * FR-027 — every regulator submission MUST be schema-validated before the network call to the trade
 * repository / ARM.
 *
 * <p>Caches compiled schemas — XSD parsing is expensive.
 */
@Component
public class XmlValidator {

  private final Map<String, Schema> cache = new HashMap<>();

  public boolean isValid(String xml, String xsdResourcePath) {
    try {
      Schema schema = cache.computeIfAbsent(xsdResourcePath, this::loadSchema);
      Validator v = schema.newValidator();
      v.validate(new StreamSource(new StringReader(xml)));
      return true;
    } catch (SAXException | java.io.IOException e) {
      return false;
    }
  }

  public void requireValid(String xml, String xsdResourcePath) throws ValidationException {
    if (!isValid(xml, xsdResourcePath)) {
      throw new ValidationException("XML failed validation against " + xsdResourcePath);
    }
  }

  private Schema loadSchema(String resourcePath) {
    try {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      var url = getClass().getResource(resourcePath);
      if (url == null) {
        throw new IllegalStateException("Schema resource not found: " + resourcePath);
      }
      return factory.newSchema(url);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load schema " + resourcePath, e);
    }
  }

  public static final class ValidationException extends Exception {
    public ValidationException(String msg) {
      super(msg);
    }
  }
}
