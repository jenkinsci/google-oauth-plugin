package com.google.jenkins.plugins.util;


import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.event.Level;

import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * @author Oleg Nenashev
 * @since TODO
 */
public class JodaDateTimeConverter implements Converter {

    private static final Logger LOGGER = Logger.getLogger(JodaDateTimeConverter.class.getName());

    @Initializer(before = InitMilestone.PLUGINS_LISTED)
    public static void initConverter() {
        // Overrides the default converters
        Jenkins.XSTREAM2.registerConverter(new JodaDateTimeConverter(), 10);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        DateTime value = (DateTime)source;
        writer.addAttribute("millis", Long.toString(value.getMillis()));
        writer.addAttribute("timezone", value.getChronology().getZone().getID());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String millis = reader.getAttribute("millis");
        if (millis != null) { // new format
            String timezone = reader.getAttribute("timezone");
            Long val = Long.parseLong(millis);
            DateTimeZone tz = timezone != null ? DateTimeZone.forID(timezone) : null;
            return new DateTime(val, tz);
        }

        //TODO: this thing may not work for other ISOChronology implementations, but in such case we will get null
        // Old format
        // <iMillis>1523889095013</iMillis>
        // <iChronology class="org.joda.time.chrono.ISOChronology" resolves-to="org.joda.time.chrono.ISOChronology$Stub" serialization="custom">
        //   <org.joda.time.chrono.ISOChronology_-Stub>
        //     <org.joda.time.tz.CachedDateTimeZone resolves-to="org.joda.time.DateTimeZone$Stub" serialization="custom">
        //       <org.joda.time.DateTimeZone_-Stub>
        //         <string>Europe/Zurich</string>
        //       </org.joda.time.DateTimeZone_-Stub>
        //     </org.joda.time.tz.CachedDateTimeZone>
        //  </org.joda.time.chrono.ISOChronology_-Stub>
        // </iChronology>
        String timezone = null;
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String name = reader.getNodeName();
            if (name.equals("iMillis")) {
                millis = reader.getValue();
            } else if (name.equals("iChronology")) {
                reader.moveDown();
                reader.moveDown();
                reader.moveDown();
                reader.moveDown();
                if ("string".equals(reader.getNodeName())) {
                    timezone = reader.getValue();
                }
                reader.moveUp();
                reader.moveUp();
                reader.moveUp();
                reader.moveUp();
            }
            reader.moveUp();
        }
        if (millis != null && timezone != null) {
            Long val = Long.parseLong(millis);
            DateTimeZone tz = DateTimeZone.forID(timezone);
            return new DateTime(val, tz);
        }

        //TODO: throw something meaningful?
        throw new IllegalStateException("Unsupported format for: " + DateTime.class + " in " + context.currentObject());
    }

    @Override
    public boolean canConvert(Class type) {
        return DateTime.class == type;
    }


}
