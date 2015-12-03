/*
* Copyright 2014 Basis Technology Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.basistech.util;

/**
 * Exception class when RLP is asked to return a named entity type when the name given does not correspond to
 * a named entity supported by RLP.
 */
public class InvalidNamedEntityTypeNameException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -4177293290385597994L;

    /**
     * Create an exception with a default message.
     */
    public InvalidNamedEntityTypeNameException() {
        super("The named entity name is not supported.");
    }

    /**
     * Create an exception with the given message.
     * 
     * @param msg Description of the exception
     */
    public InvalidNamedEntityTypeNameException(String msg) {
        super(msg);
    }

}
