/*
 * Copyright 2018 CloudBees, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.util;

import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;

/**
 * Performs JEP-200-safe conversion of {@link DateTime} classes.
 * @author Oleg Nenashev
 * @since TODO(oleg_nenashev): Add once the target release is defined.
 */
public class JodaDateTimeConverter implements Converter {

    private static final Logger LOGGER = Logger.getLogger(
            JodaDateTimeConverter.class.getName());

    @Initializer(before = InitMilestone.PLUGINS_LISTED)
    public static void initConverter() {
        // Overrides the default converters, runs before the JEP-200 blacklist
        Jenkins.XSTREAM2.registerConverter(new JodaDateTimeConverter());
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
                        MarshallingContext context) {
        DateTime value = (DateTime) source;
        writer.addAttribute("millis",
                Long.toString(value.getMillis()));
        writer.addAttribute("timezone",
                value.getChronology().getZone().getID());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
        String millis = reader.getAttribute("millis");
        if (millis != null) { // new format
            String timezone = reader.getAttribute("timezone");
            Long val = Long.parseLong(millis);
            DateTimeZone tz = timezone != null
                    ? DateTimeZone.forID(timezone) : null;
            return new DateTime(val, tz);
        }

        //TODO(oleg-nenashev): this thing may not work for other
        // ISOChronology implementations, but in such case we will get null.
        // Null will enforce the token refresh

        // Old format
        // <iMillis>1523889095013</iMillis>
        // <iChronology class="org.joda.time.chrono.ISOChronology"
        //              resolves-to="org.joda.time.chrono.ISOChronology$Stub"
        //              serialization="custom">
        //   <org.joda.time.chrono.ISOChronology_-Stub>
        //     <org.joda.time.tz.CachedDateTimeZone
        //              resolves-to="org.joda.time.DateTimeZone$Stub"
        //              serialization="custom">
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

        //TODO(oleg-nenashev): throw something meaningful?
        throw new IllegalStateException("Unsupported format for: " +
                DateTime.class + " in " + context.currentObject());
    }

    @Override
    public boolean canConvert(Class type) {
        return DateTime.class == type;
    }


}
