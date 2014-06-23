/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.rosette.internal.take5build;

import com.google.common.base.Throwables;
import com.google.common.io.CharSource;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
  */
public class ControlFile {


    class Processor implements LineProcessor<List<InputSpecification>> {
        private int lineNumber;
        private List<InputSpecification> results;

        @Override
        public boolean processLine(String line) throws IOException {
            if (line.startsWith("#")) {
                return true;
            }

            int eqidx = line.indexOf('=');
            if (eqidx < 1) {
                throw Throwables.propagate(new InputFileException("No key on line " + lineNumber));
            }
            String key = line.substring(0, eqidx);
            String value = line.substring(eqidx + 1);

            InputSpecification spec = new InputSpecification();

            /*
            Where I<kind> is C<0> for no escapes, C<1> for backslash escape sequences
(see B<-q>), or C<2> for sortable escape sequences (see B<-u>).  If B<-q>
or B<-u> were given on the command line, this defaults to that setting,
otherwise it defaults to no escapes
             */
            if ("ESCAPE".equals(key)) {
                if ("0".equals(value)) {
                    spec.simpleKeys = true;
                } else if ("1".equals(value)) {
                    // OK as we are
                } else {
                    throw Throwables.propagate(new InputFileException(String.format("Unsupported ESCAPE value %s at line %d", value, lineNumber)));
                }
            } else if ("MIN_VERSION".equals(key)) {
                spec.minVersion = Integer.parseInt(value);
            } else if ("MAX_VERSION".equals(key)) {
                spec.maxVersion = Integer.parseInt(value);
            } else if ("NAME".equals(key)) {
                spec.entrypointName = value;
            } else if ("PATH".equals(key)) {
                spec.inputFile = new File(value);
            } else if ("VALUE_MODE".equals(key)) {
                spec.defaultMode = value;
            } else if ("VERSION".equals(key)) {
                spec.minVersion = Integer.parseInt(value);
                spec.maxVersion = spec.minVersion;
            } else if ("FLAGS".equals(key)) {
                spec.contentFlags = Integer.parseInt(value);
            }
            results.add(spec);
            return true;
        }

        @Override
        public List<InputSpecification> getResult() {
            return results;
        }
    }

    List<InputSpecification> read(CharSource source) throws IOException, InputFileException {
        try {
            return source.readLines(new Processor());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InputFileException) {
                throw (InputFileException)e.getCause();
            }
            throw e;
        }
    }
}
