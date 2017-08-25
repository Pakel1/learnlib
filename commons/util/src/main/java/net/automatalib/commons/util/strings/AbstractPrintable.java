/* Copyright (C) 2013-2017 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
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
package net.automatalib.commons.util.strings;

import java.io.IOException;

/**
 * Abstract base class for printables.
 * <p>
 * Besides implementing the {@link Printable} interface, it provides a standard {@link #toString()} implementation using
 * a {@link StringBuilder} and the {@link #print(Appendable)} method.
 *
 * @author Malte Isberner
 */
public abstract class AbstractPrintable implements Printable {

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return toString(this);
    }

    public static String toString(Printable p) {
        StringBuilder sb = new StringBuilder();
        try {
            p.print(sb);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IOException thrown during operation on StringBuilder.", e);
            // THIS SHOULD NOT HAPPEN
            // since the StringBuilder methods do not throw.
        }
        return sb.toString();
    }
}
